/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */
package org.pentaho.di.job.entries.publish;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.success.JobEntrySuccess;
import org.pentaho.di.job.entry.JobEntryCopy;

/**
 * Tests the JobEntryDatasourcePublish by setting up a Job and running it.
 * 
 * @author Benny
 *
 */
public class JobEntryDatasourcePublishInJobTest {
  private Job job;
  private JobEntryDatasourcePublish publishJobEntry;
  private JobEntryCopy publishCopy;
  private DataSourcePublishModel model;
  private BiServerConnection serverModel;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    if ( !KettleClientEnvironment.isInitialized() ) {
      KettleClientEnvironment.init();
    }
    PluginRegistry.addPluginType( StepPluginType.getInstance() );
    PluginRegistry.init();
    if ( !Props.isInitialized() ) {
      Props.init( 0 );
    }
  }

  @Before
  public void setUp() throws Exception {
    model = new DataSourcePublishModel();
    serverModel = new BiServerConnection();
    model.setBiServerConnection( serverModel );

    // Job Setup
    job = new Job( null, new JobMeta() );
    // Add start job entry
    JobEntrySpecial start = new JobEntrySpecial( "START", true, false );
    JobEntryCopy startCopy = new JobEntryCopy( start );
    startCopy.setDrawn();
    job.getJobMeta().addJobEntry( startCopy );
    start.setParentJob( job );

    // Add Publish job entry
    publishJobEntry = new JobEntryDatasourcePublish();
    publishJobEntry.setName( "Publish Me" );
    publishJobEntry.setDataSourcePublishModel( model );
    publishCopy = new JobEntryCopy( publishJobEntry );
    publishCopy.setDrawn();
    job.getJobMeta().addJobEntry( publishCopy );
    publishJobEntry.setParentJob( job );

    JobHopMeta hop2 = new JobHopMeta( startCopy, publishCopy );
    job.getJobMeta().addJobHop( hop2 );
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testErrorHop() throws Exception {
    JobEntrySuccess jobEntrySuccess = new JobEntrySuccess();
    JobEntryCopy copy = new JobEntryCopy( jobEntrySuccess );
    copy.setDrawn();
    job.getJobMeta().addJobEntry( copy );
    jobEntrySuccess.setParentJob( job );

    // Create a error hop that leads to the job success
    JobHopMeta hop2 = new JobHopMeta( publishCopy, copy );
    hop2.setEvaluation( false );
    job.getJobMeta().addJobHop( hop2 );

    job.run();
    // Publish job entry will fail but it'll follow the error hop to the job success entry
    // and thus the whole job will succeed with no errors.
    assertTrue( job.getResult().getResult() );
    assertEquals( 0, job.getResult().getNrErrors() );

    // Now switch the hop to success so that the job fails immediately in the publish job entry
    hop2.setEvaluation( true );
    job.run();
    assertFalse( job.getResult().getResult() );
    assertEquals( 1, job.getResult().getNrErrors() );
  }

}