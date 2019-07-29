
package com.ice.jcvsii;

public
class		CVSThread
extends		Thread
	{
	protected Monitor		monitor;
	protected Runnable		subRunner;


	/**
	 * We severely restrict construction!
	 */
	private CVSThread() { }

	private CVSThread( final String name ) { }

	private CVSThread( final Runnable runner ) { }

	private CVSThread( final ThreadGroup group, final String name ) { }

	private CVSThread( final ThreadGroup group, final Runnable runner, final String name ) { }


	public
	CVSThread( final String name, final Runnable runner, final Monitor monitor )
		{
		super( name );
		this.monitor = monitor;
		this.subRunner = runner;
		}

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
		public void
			threadStarted();

		public void
			threadCanceled();

		public void
			threadFinished();
		}

	}

