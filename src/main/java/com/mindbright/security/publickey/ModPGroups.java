/******************************************************************************
 *
 * Copyright (c) 1999-2001 AppGate AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 1.3, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * AppGate AB, Stora Badhusgatan 18-20, 41121 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.security.publickey;

import java.math.BigInteger;

public final class ModPGroups {

    // http://www.ietf.org/rfc/rfc2409.txt

    public final static BigInteger oakleyGroup1P =
	new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
		       "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
		       "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
		       "E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF", 16);

    public final static BigInteger oakleyGroup1G = BigInteger.valueOf(2);

    public final static BigInteger oakleyGroup2P =
	new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
		       "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
		       "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
		       "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
		       "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381" +
		       "FFFFFFFFFFFFFFFF", 16);

    public final static BigInteger oakleyGroup2G = oakleyGroup1G;

    // http://www.ietf.org/internet-drafts/draft-ietf-ipsec-ike-modp-groups-01.txt

    public final static BigInteger oakleyGroup5P =
	new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
		       "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
		       "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
		       "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
		       "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
		       "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
		       "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
		       "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF", 16);

    public final static BigInteger oakleyGroup5G = oakleyGroup1G;

}
