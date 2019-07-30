
package com.ice.jcvsii;

class		CVSThread
extends		Thread
	{
	private Monitor		monitor;
	private Runnable		subRunner;


	/**
	 * We severely restrict construction!
	 */
	private CVSThread() { }

	private CVSThread( final String name ) { }

	private CVSThread( final Runnable runner ) { }

	private CVSThread( final ThreadGroup group, final String name ) { }

	private CVSThread( final ThreadGroup group, final Runnable runner, final String name ) { }


	CVSThread( final String name, final Runnable runner, final Monitor monitor )
		{
		super( name );
		this.monitor = monitor;
		this.subRunner = runner;
		}

	@Override
	public void
	run()
		{
		this.monitor.threadStarted();

		this.subRunner.run();

		this.monitor.threadFinished();
		}

	public
	interface	Monitor
		{
		void
			threadStarted();

		void
			threadCanceled();

		void
			threadFinished();
		}

	}

