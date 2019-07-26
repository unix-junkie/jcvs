/*
** Copyright (c) 1997 by Timothy Gerard Endres
** All rights reserved.
*/

#include "JNIDDE.h"

#include <wtypes.h>
#include <winbase.h>
#include <stdio.h>
#include <string.h>

#include <DDEML.H>


const char	*getDDEErrorString( UINT errCode );
void		dde_list_targets( int argc, char *argv[] );
void		dde_execute_command( int argc, char *argv[] );
jint		throwException( JNIEnv *env, UINT errCode, char *message );

HDDEDATA CALLBACK
	MyDDECallback (
			UINT type,        /* transaction type                 */
			UINT fmt,         /* clipboard data format            */
			HCONV hconv,      /* handle of conversation           */
			HSZ hsz1,         /* handle of string                 */
			HSZ hsz2,         /* handle of string                 */
			HDDEDATA hData,   /* handle of global memory object   */
			DWORD dwData1,    /* transaction-specific data        */
			DWORD dwData2     /* transaction-specific data        */
			);


void
jStringToAscii
		( JNIEnv *env, jstring jStr, char *asciiBuf, int asciiLen )
	{
	int			i;
	int			utfLen;
	jboolean	isCopy;
	const char	*utfBuf;

	utfLen = (*env)->GetStringUTFLength( env, jStr );
	utfBuf = (*env)->GetStringUTFChars( env, jStr, &isCopy );

	for ( i = 0 ; i < utfLen && i < (asciiLen - 1) ; ++i )
		asciiBuf[i] = utfBuf[i];

	asciiBuf[i] = '\0';

	(*env)->ReleaseStringUTFChars( env, jStr, utfBuf );
	}

JNIEXPORT jboolean JNICALL
Java_com_ice_jni_dde_JNIDDE_ddeExecute
		( JNIEnv *env, jclass myClass,
			jstring service, jstring topic,
				jstring command, jboolean isAsync )
	{
	HCONV		hConv; 
	DWORD		afCmd;
	PFNCALLBACK	lpDdeProc;
	UINT		err = 0;
	DWORD		ddeInst = 0;
    HSZ			hszService;
    HSZ			hszTopic;
    HDDEDATA	hData;
    DWORD		dwResult;
	DWORD		timeout;
	jint		utfLen;
	char		*serviceStr, *topicStr, *commandStr;

	utfLen = (*env)->GetStringUTFLength( env, service );
	serviceStr = malloc( utfLen + 2 );
	jStringToAscii( env, service, serviceStr, utfLen + 2 );

	utfLen = (*env)->GetStringUTFLength( env, topic );
	topicStr = malloc( utfLen + 2 );
	jStringToAscii( env, topic, topicStr, utfLen + 2 );

	utfLen = (*env)->GetStringUTFLength( env, command );
	commandStr = malloc( utfLen + 2 );
	jStringToAscii( env, command, commandStr, utfLen + 2 );
	
	afCmd = APPCLASS_STANDARD
			| APPCMD_CLIENTONLY
			| CBF_FAIL_ALLSVRXACTIONS
			| CBF_SKIP_REGISTRATIONS
			| CBF_SKIP_UNREGISTRATIONS;

	lpDdeProc =
		(PFNCALLBACK)
			MakeProcInstance( (FARPROC)MyDDECallback, hInst );

	err = DdeInitialize( (DWORD FAR *)&ddeInst, lpDdeProc, afCmd, 0L );
	if ( err != 0 )
		{
		throwException( env, err, "DdeInitialize()" );
		return JNI_FALSE;
		}
	
    hszService =
		DdeCreateStringHandle
			( ddeInst, (LPTSTR) serviceStr, CP_WINANSI );

    hszTopic =
		DdeCreateStringHandle
			( ddeInst, (LPTSTR) topicStr, CP_WINANSI );

    if (hszService == (HSZ)NULL || hszTopic == (HSZ)NULL)
		{
		err = DdeGetLastError( ddeInst );
		DdeUninitialize( ddeInst );
		throwException( env, err, "DdeCreateStringHandle()" );
		return JNI_FALSE;
		}

    hConv = DdeConnect(
        ddeInst,                /* instance identifier                */
        hszService,             /* server's service name              */
        hszTopic,               /* topic name                         */
        NULL                    /* use default CONVCONTEXT            */
		);

    if ( hConv == (HCONV)NULL )
		{
		err = DdeGetLastError( ddeInst );
		DdeUninitialize( ddeInst );
		throwException( env, err, "DdeConnect()" );
		return JNI_FALSE;
		}

	timeout = isAsync ? TIMEOUT_ASYNC : 3000;

    hData =
		DdeClientTransaction(
			(LPBYTE) commandStr,  /* pass data to server    */
			strlen( commandStr ), /* bytes of data          */
			hConv,                /* conversation handle    */
			(HSZ)NULL,            /* item name              */
			CF_TEXT,              /* clipboard format       */
			XTYP_EXECUTE,         /* start an advise loop   */
			timeout,              /* time-out               */
			&dwResult );          /* points to result flags */

    /* hData should be non-zero for successful transactions */
    if ( hData == 0 )
		{
		err = DdeGetLastError( ddeInst );
		DdeDisconnect( hConv ); 
		DdeUninitialize( ddeInst );
		throwException
			( env, err,
				"DdeClientTransaction(XTYP_EXECUTE)" );
		return JNI_FALSE;
		}

	if ( ! isAsync && hData != 0 )
		DdeFreeDataHandle( hData );

	DdeDisconnect( hConv ); 
	DdeUninitialize( ddeInst );

	return JNI_TRUE;
	}

JNIEXPORT void JNICALL
JNICALL Java_com_ice_jni_dde_JNIDDE_shellExecute
		( JNIEnv *env, jclass myClass,
			jstring operation, jstring fileName,
			jstring parameter, jstring defaultDir,
			jint showCmd )
	{
	BOOL boolResult;
	SHELLEXECUTEINFO sInfo;

	jint		utfLen;
	char		*operationStr, *fileNameStr,
				*paramStr, *defDirStr;

	utfLen = (*env)->GetStringUTFLength( env, operation );
	operationStr = malloc( utfLen + 2 );
	jStringToAscii( env, operation, operationStr, utfLen + 2 );

	utfLen = (*env)->GetStringUTFLength( env, fileName );
	fileNameStr = malloc( utfLen + 2 );
	jStringToAscii( env, fileName, fileNameStr, utfLen + 2 );

	if ( parameter == NULL )
		{
		paramStr = NULL;
		}
	else
		{
		utfLen = (*env)->GetStringUTFLength( env, parameter );
		paramStr = malloc( utfLen + 2 );
		jStringToAscii( env, parameter, paramStr, utfLen + 2 );
		}
	
	utfLen = (*env)->GetStringUTFLength( env, defaultDir );
	defDirStr = malloc( utfLen + 2 );
	jStringToAscii( env, defaultDir, defDirStr, utfLen + 2 );
	
	sInfo.cbSize = sizeof( sInfo );
	sInfo.fMask = SEE_MASK_FLAG_DDEWAIT | SEE_MASK_FLAG_NO_UI;
	sInfo.hwnd = GetDesktopWindow();
	sInfo.lpVerb = (LPCTSTR) operationStr;
	sInfo.lpFile = (LPCTSTR) fileNameStr;
	sInfo.lpParameters = (LPCTSTR) paramStr;
	sInfo.lpDirectory = (LPCTSTR) defDirStr;
	sInfo.nShow = (INT)showCmd;

	boolResult =
		ShellExecuteEx( (LPSHELLEXECUTEINFO) &sInfo );

	if ( ! boolResult )
		{
		LPVOID lpMsgBuf;

		FormatMessage( 
			FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
			NULL,
			GetLastError(),
			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
			(LPTSTR) &lpMsgBuf,
			0,
			NULL 
			);

		throwException( env, (UINT)GetLastError(), lpMsgBuf );

		LocalFree( lpMsgBuf );
		}
	}

jint
throwException( JNIEnv *env, UINT errCode, char *message )
	{
	jclass		exClass;
	char		exMsg[512];

	exClass =
		(*env)->FindClass
			( env, "com/ice/jni/dde/DDEException" );

	sprintf(
		exMsg, "%.200s: errCode %d (%.100s)",
			message, errCode, getDDEErrorString( errCode ) );

	return (*env)->ThrowNew( env, exClass, exMsg );
	}

const char *
getDDEErrorString( UINT errCode )
	{
	switch ( errCode )
		{
		case DMLERR_ADVACKTIMEOUT:
			return "advise ACK timeout";
		case DMLERR_BUSY:
			return "service busy";
		case DMLERR_DATAACKTIMEOUT:
			return "data ACK timeout";
		case DMLERR_DLL_NOT_INITIALIZED:
			return "dll not initialized";
		case DMLERR_DLL_USAGE:
			return "dll usage error";
		case DMLERR_EXECACKTIMEOUT:
			return "execute ACK timeout";
		case DMLERR_INVALIDPARAMETER:
			return "invaliad parameter";
		case DMLERR_LOW_MEMORY:
			return "low memory";
		case DMLERR_MEMORY_ERROR:
			return "memory error";
		case DMLERR_NOTPROCESSED:
			return "not processed by service";
		case DMLERR_NO_CONV_ESTABLISHED:
			return "no conversation established";
		case DMLERR_POKEACKTIMEOUT:
			return "poke ACK timeout";
		case DMLERR_POSTMSG_FAILED:
			return "post message failed";
		case DMLERR_REENTRANCY:
			return "reentrancy error";
		case DMLERR_SERVER_DIED:
			return "server died";
		case DMLERR_SYS_ERROR:
			return "system error";
		case DMLERR_UNADVACKTIMEOUT:
			return "unadvise ACK timeout";
		case DMLERR_UNFOUND_QUEUE_ID:
			return "queue id not found";

		// S H E L L    E X E C U T E    E R R O R S
		case 0:
			return "operating system is out of memory or resources";
		case ERROR_FILE_NOT_FOUND:
			return "specified file was not found";
		case ERROR_PATH_NOT_FOUND:
			return "specified path was not found";
		case ERROR_BAD_FORMAT:
			return ".EXE file is invalid";
		case SE_ERR_ACCESSDENIED:
			return "operating system denied access to the specified file";
		case SE_ERR_ASSOCINCOMPLETE:
			return "filename association is incomplete or invalid";
		case SE_ERR_DDEBUSY:
			return "DDE transaction failed, busy";
		case SE_ERR_DDEFAIL:
			return "DDE transaction failed";
		case SE_ERR_DDETIMEOUT:
			return "DDE transaction failed, timed out";
		case SE_ERR_DLLNOTFOUND:
			return "specified dynamic-link library was not found";
		case SE_ERR_NOASSOC:
			return "no application association found for filename extension";
		case SE_ERR_OOM:
			return "not enough memory to complete the DDE operation";
		case SE_ERR_SHARE:
			return "sharing violation occurred";
		}

	return "unknown error code";
	}

#ifdef NEVER_DEFINED 

void
dde_list_targets( int argc, char *argv[] )
	{
	int			pSize;
	HCONVLIST	hconvList; /* conversation list       */  
	HCONV		hConv; 
	DWORD		afCmd;
	PFNCALLBACK	lpDdeProc;
	UINT		err = 0;
	DWORD		ddeInst = 0;
	HSZ			hszSysTopic;
	CONVINFO	ci;         /* holds conversation data */ 
	char		partner[1024];

    if ( debug )
		fprintf( stderr, "dde_list_targets: listing targets\n" );

	afCmd = APPCLASS_STANDARD
			| APPCMD_CLIENTONLY
			| CBF_FAIL_ALLSVRXACTIONS
			| CBF_SKIP_REGISTRATIONS
			| CBF_SKIP_UNREGISTRATIONS;

	lpDdeProc =
		(PFNCALLBACK)
			MakeProcInstance( (FARPROC)MyDDECallback, hInst );

	err = DdeInitialize( (DWORD FAR *)&ddeInst, lpDdeProc, afCmd, 0L );
	if ( err != 0 )
		{
		if ( debug )
			fprintf( stderr, "DdeInitialize failed with error %d.\n", err);
		exit(2);
		}
	
    if ( debug )
		fprintf( stderr, "initialized, creating some strings...\n" );

	hszSysTopic =
		DdeCreateStringHandle
			( ddeInst, (LPTSTR) "System", CP_WINANSI );

    if ( hszSysTopic == (HSZ)NULL )
		{
        if ( debug )
			fprintf( stderr, "Unable to create DDE string handles.\n" );

		DdeUninitialize( ddeInst );
		exit(3);
		}

    if ( debug )
		fprintf( stderr, "strings done, getting connection list...\n" );

	hconvList =
		DdeConnectList( ddeInst, NULL, hszSysTopic, NULL, NULL ); 

    if ( hconvList == (HCONV)NULL )
		{
        if ( debug )
			fprintf( stderr, "DdeConnectList() failed.\n" );

		DdeUninitialize( ddeInst );
		exit(4);
		}

    if ( debug )
		fprintf( stderr, "connection list retrieved, printing list...\n" );

	for ( hConv = 0 ; ; )
		{
		hConv = DdeQueryNextServer( hconvList, hConv );
		if ( hConv == NULL)
			break;
		
		ci.cb = sizeof( ci ); 
		if ( ! DdeQueryConvInfo( hConv, QID_SYNC, (PCONVINFO)&ci ) )
			{
			if ( debug )
				fprintf( stderr, "DdeQueryConvInfo() failed...\n" );
			}
		else
			{
			DdeKeepStringHandle( ddeInst, ci.hszSvcPartner ); 
			DdeKeepStringHandle( ddeInst, ci.hszServiceReq ); 
		
			pSize = 
				DdeQueryString(
					ddeInst, ci.hszSvcPartner,
					(LPTSTR)partner, 1000, CP_WINANSI );

			printf( "Partner [%d]'%s'\n", pSize, partner );
			}	
		} 

	DdeDisconnectList( hconvList ); 

	DdeUninitialize( ddeInst );

	exit(0);
	}

#endif


HDDEDATA CALLBACK
MyDDECallback (
		UINT type,        /* transaction type                 */
		UINT fmt,         /* clipboard data format            */
		HCONV hconv,      /* handle of conversation           */
		HSZ hsz1,         /* handle of string                 */
		HSZ hsz2,         /* handle of string                 */
		HDDEDATA hData,   /* handle of global memory object   */
		DWORD dwData1,    /* transaction-specific data        */
		DWORD dwData2     /* transaction-specific data        */
		)
	{
	switch (type)
		{
		case XTYP_ADVDATA:
			return (HDDEDATA) DDE_FACK;

		case XTYP_REGISTER:
		case XTYP_UNREGISTER:
		case XTYP_XACT_COMPLETE:
		case XTYP_DISCONNECT:
		default:
			return (HDDEDATA) NULL;
		}
	}

