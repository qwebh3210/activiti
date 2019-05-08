package junit;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.junit.Test;

public class TestActiviti {

	/**
	 * 使用代码创建工作流需要的23张表
	 */
	@Test
	public void createTable(){
		//创建ProcessEngineConfiguration单例对象
		ProcessEngineConfiguration processEngineConfiguration 
			= ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
		
		//用processEngineConfiguration配置数据库连接信息
		processEngineConfiguration.setJdbcDriver("com.mysql.jdbc.Driver");
		processEngineConfiguration.setJdbcUrl("jdbc:mysql://localhost:3306/activitidb?useUnicode=true&characterEncoding=utf8");
		processEngineConfiguration.setJdbcUsername("root");
		processEngineConfiguration.setJdbcPassword("root");
		
		/*
		* DB_SCHEMA_UPDATE_FALSE 不创建表，需要之前已创建
		* DB_SCHEMA_UPDATE_CREATE_DROP 先删除表再创建表
		* DB_SCHEMA_UPDATE_TRUE 如表不存在自动创建表
		*/
		//用processEngineConfiguration配置创建工作流相关表的方式
		processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
		
		//用processEngineConfiguration创建工作流核心对象——流程引擎对象：processEngine
		//processEngine对象被创建的同时，就会创建工作流相关的23张表了
		ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
		
		System.out.println("processEngine="+processEngine);
	}
	
	/**
	 * 使用配置文件创建工作流需要的23张表方案一
	 */
	@Test
	public void createTable_2(){
		//从activiti.cfg.xml配置文件创建ProcessEngineConfiguration对象
		ProcessEngineConfiguration processEngineConfiguration = 
				ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("activiti.cfg.xml");
		
		//processEngine对象被创建的同时，就会创建工作流相关的23张表了
		ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
		
		System.out.println("processEngine="+processEngine);
	}
	
	/**
	 * 使用配置文件创建工作流需要的23张表方案二
	 */
	@Test
	public void createTable_3(){
		/*
		 * 使用该API会默认读取classpath（即src/main/resources）下名为
		 * activiti.cfg.xml的配置文件，依此来创建ProcessEngine对象
		 */
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		
		System.out.println("processEngine="+processEngine);
	}
}
