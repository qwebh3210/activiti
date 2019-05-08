package com.buhao.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.history.HistoricVariableInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.buhao.bean.Person;

/**
 * 流程引擎对象七大service测试
 * @author Hao
 */
public class TestProcessEngineService {

	//创建流程引擎对象
	ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
	
	/**
	 * 部署流程定义
	 * 方式1：通过流程图（bpmn和png文件）部署
	 */
	@Test
	public void deploymentProcessDefinition1(){
		Deployment deployment = processEngine.getRepositoryService()//获取流程定义service
						.createDeployment()//创建一个部署对象
						.name("leaveProcess")//添加部署名称
						.addClasspathResource("diagrams/leaveProcess.bpmn")
						.addClasspathResource("diagrams/leaveProcess.png")//从classpath的资源中加载流程图及流程图对应的png，一次只能加载一个文件
						.deploy();//完成部署
		
		System.out.println("部署id：" + deployment.getId());
		System.out.println("部署name：" + deployment.getName());
	}
	
	/**
	 * 部署流程定义
	 * 方式2：通过流程图（bpmn和png文件）打成的zip包部署
	 */
	@Test
	public void deploymentProcessDefinition2(){
		//首先要把bpmn和png打成zip包，然后放到项目路径下
		//获取zip包的输入流
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("diagrams/leaveProcess.zip");
		ZipInputStream zipInputStream = new ZipInputStream(inputStream);
		
		Deployment deployment = processEngine.getRepositoryService()//获取流程定义service
						.createDeployment()//创建一个部署对象
						.name("leaveProcess4")//添加部署名称
						.addZipInputStream(zipInputStream)//指定zip包的输入流
						.deploy();//完成部署
		
		System.out.println("部署id：" + deployment.getId());
		System.out.println("部署name：" + deployment.getName());
	}
	
	/**
	 * 启动流程实例
	 */
	@Test
	public void startProcessInstance(){
		//流程定义对象key值
		String processDefinitionKey = "leaveProcessId";
		
		//启动流程
		ProcessInstance processInstance = processEngine.getRuntimeService()//创建管理流程实例和执行对象的service
						.startProcessInstanceByKey(processDefinitionKey);//使用流程定义对象的key值启动实例，有多个key值时，会使用版本号最新的流程定义对象启动;也可以用流程定义id启动实例，并且流程定义id是唯一的，但是实际中，key是已知的，而流程定义id是生成的，需要单独查询，逻辑上用key更符合现实情况
						
/*		//启动流程时携带变量
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("startVar", true);
		ProcessInstance processInstance = processEngine.getRuntimeService()
						.startProcessInstanceByKey(processDefinitionKey, map);*/
		
		System.out.println("流程实例id：" + processInstance.getId());
		System.out.println("流程定义id：" + processInstance.getProcessDefinitionId());
	}
	
	/**
	 * 完成个人任务
	 */
	@Test
	public void completeMyPersonalTask(){
		//任务id
		String taskId = "7504";
		//封装流条件
		Map<String,Object> condition = new HashMap<String,Object>();
		condition.put("type", "0");
		
		//创建管理任务的service
		TaskService taskService = processEngine.getTaskService();
		//签收任务：签收后执行人assignee为签收人，没有签收的话assignee为空；
		//签收后act_hi_taskinst的claim_time会填充当前时间，但只是签收并不代表任务完成
		//已经签收的任务无法再次指定人签收
		//使用api：taskService.setAssignee(taskId, "wangwu");
		//也可以达成类似claim的功效，不同的是，setAssignee可以反复设置执行人，
		//而且完成任务后claim_time是没有值的
		taskService.claim(taskId, "zhangsan");
		//委托任务：a委托任务给b，b成了新的执行人assignee，a成了任务的所有人owner，
		//b可以继续委托给其他人，但只会更换执行人，owner永远还是a
		//在没有人签收的前提下直接委托，那么被委托人就是执行人assignee
		taskService.delegateTask(taskId, "zhaoliu");
		//完成任务,并传入流条件决定下一任务走向
		taskService.complete(taskId, condition);
	}
	
	/**
	 * 完成组任务
	 */
	@Test
	public void completeCandidateUsersTask(){
		//任务id
		String taskId = "14005";
		//封装流条件和候选人给下一个任务用
		Map<String,Object> condition = new HashMap<String,Object>();
		condition.put("type", "0");	
		condition.put("userIds", "lisi,wangwu");
		condition.put("completeVar", false);
		
		//创建管理任务的service
		TaskService taskService = processEngine.getTaskService();
		//为本任务添加候选人
//		taskService.addCandidateUser(taskId, "liujiu");
		//为本任务删除候选人
//		taskService.deleteCandidateUser(taskId, "qianqi");
		//虽然程序不会强制要求有人签收，也不会要求一定是候选人队列中选人签收，
		//但一般的业务逻辑都是要有签收人，且签收人是候选人中的一个
		//上面封装的候选人是给下一任务用的，这里签收人应该是上一任务传给本任务的候选人中的一个
		taskService.claim(taskId, "zhaoliu");
		//完成任务,并传入流条件决定下一任务走向
		taskService.complete(taskId, condition);
	}
	
	/**
	 * 查看当前任务
	 */
	@Test
	public void findMyPersonalTask(){
		//创建管理任务的service
		TaskService taskService = processEngine.getTaskService();
		//创建任务查询对象，注：一个TaskQuery对象只能调用一次查询api，多次调用需要多个TaskQuery对象，否则查出的结果会有误
		TaskQuery tq = taskService.createTaskQuery();
		
		//注：这里的所有任务查询都是查询当前任务，即当前任务未完成，完成就成了历史任务查不到了
		//根据指定执行人进行任务查询：只有当当前任务已被zhangsan签收或zhangsan为执行人（）
		List<Task> list = tq.taskAssignee("zhangsan").list();
/*		//根据指定组任务的候选人进行任务查询：只有当当前任务候选人中有wangwu，且还没有人签收或没有执行人（当前任务未完成，完成就成了历史任务查不到了）
		//签收代表候选人已没有意义，签收并完成后本任务的候选人直接清空，只能在历史流程人员表才找得到
		List<Task> list2 = tq.taskCandidateUser("wangwu").list();
		//根据流程定义id查询任务,并根据任务创建时间升序排列，从下标0取数据（即第1条），往后取1条
		List<Task> list = tq.processDefinitionId("leaveProcessId:6:9204").orderByTaskCreateTime().asc().listPage(0,1);
		//根据流程实例id查询任务数量
		long cnt = tq.processInstanceId("11001").count();
		//根据执行id查询任务
		Task task = tq.executionId("11001").singleResult();*/
		
		//取list的数据进行打印，其他结果打印类同
		if(null != list && list.size()>0){
			for(Task task:list){
				System.out.println("任务id：" + task.getId());
				System.out.println("任务名称：" + task.getName());
				System.out.println("任务创建时间：" + task.getCreateTime());
				System.out.println("任务执行人：" + task.getAssignee());
				System.out.println("流程实例id：" + task.getProcessInstanceId());
				System.out.println("执行对象id：" + task.getExecutionId());
				System.out.println("流程定义id：" + task.getProcessDefinitionId());
			}
		}
	}
	
	/**
	 * 查看历史数据
	 */
	@Test
	public void findHistoryData(){
		//创建与历史数据相关的service
		HistoryService historyService = processEngine.getHistoryService();
		
		//查询历史任务
		List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()//创建历史任务实例查询对象
														.taskAssignee("zhangsan")//根据任务执行人查询历史任务
														.list();
		
		if(null != list && 0 < list.size()){
			for(HistoricTaskInstance hti : list){
				System.out.println(hti.getId() + "	" + hti.getName() + "	" + hti.getProcessInstanceId() + "	" + hti.getStartTime() + "	" + hti.getEndTime() + "	" + hti.getDurationInMillis());
				System.out.println("######################");
			}
		}		
		
		//查询历史流程实例
		HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()//创建历史流程实例查询对象
													.processInstanceId("8701")
													.singleResult();
		
		System.out.println(hpi.getId() + "	" + hpi.getProcessDefinitionId() + "	" + hpi.getStartTime() + "	" + hpi.getEndTime() + "	" + hpi.getDurationInMillis());
		System.out.println("######################");
		
		//查询历史活动
		List<HistoricActivityInstance> list2 = historyService.createHistoricActivityInstanceQuery()//创建历史活动实例查询对象
															.processInstanceId("8701")
															.orderByHistoricActivityInstanceStartTime().asc()
															.list();
		
		if(null != list2 && 0 < list2.size()){
			for(HistoricActivityInstance hai : list2){
				System.out.println(hai.getId() + "	" + hai.getProcessInstanceId() + "	" + hai.getActivityType() + "	" + hai.getStartTime() + "	" + hai.getEndTime());
				System.out.println("######################");
			}
		}
	}
	
	/**
	 * 根据流程实例id查看流程状态
	 */
	@Test
	public void queryProcessState(){
		//流程实例id
		String processInstanceId = "1501";
		
		ProcessInstance pi = processEngine.getRuntimeService()
								.createProcessInstanceQuery()
								.processInstanceId(processInstanceId)
								.singleResult();
		
		if(null != pi){
			System.out.println("当前流程在：" + pi.getActivityId());
		}else{
			System.out.println("流程已结束");
		}
	}
	
	/**
	 * 查询流程定义
	 */
	@Test
	public void findProcessDefinition(){
		//创建流程定义service
		RepositoryService repositoryService = processEngine.getRepositoryService();
		//创建一个流程定义查询,注：每个ProcessDefinitionQuery对象只能用于一次查询，连续多次查询（如把下列注释都解开执行），查询结果是有误的；
		//如需多次查询，每次查询都用repositoryService.createProcessDefinitionQuery()去创建一个ProcessDefinitionQuery对象
		ProcessDefinitionQuery pdq = repositoryService.createProcessDefinitionQuery();
		
		//使用部署对象id查询,返回单实例流程定义对象
		ProcessDefinition pd1 = pdq.deploymentId("101").singleResult();
		//使用流程定义id查询，返回单实例流程定义对象
		ProcessDefinition pd2 = pdq.processDefinitionId("leaveProcessId:2:104").singleResult();
		//使用流程定义key值查询，并根据版本号升序排列，返回查询结果集数量
		long cnt = pdq.processDefinitionKey("leaveProcessId").count();
		//使用流程定义名称模糊查询，并根据流程定义id降序排列，返回多实例流程定义集合
		List<ProcessDefinition> list1 = pdq.processDefinitionNameLike("leaveProcess").orderByProcessDefinitionId().desc().list();
		//使用流程定义key值查询，并根据版本号升序排列，从下标1取数据（即第2条），往后取2条
		List<ProcessDefinition> list2 = pdq.processDefinitionKey("leaveProcessId").orderByProcessDefinitionVersion().asc().listPage(1, 2);
		
		//取list2的数据进行打印，其他结果打印类同
		if(null != list2 && 0 < list2.size()){
			for(ProcessDefinition pd : list2){
				System.out.println("流程定义id：" + pd.getId());
				System.out.println("流程定义名称：" + pd.getName());
				System.out.println("流程定义key：" + pd.getKey());
				System.out.println("流程定义版本：" + pd.getVersion());
				System.out.println("资源bpmn全局定义名：" + pd.getResourceName());
				System.out.println("资源png全局定义名：" + pd.getDiagramResourceName());
				System.out.println("部署对象id：" + pd.getDeploymentId());
				System.out.println("#########################################");
			}
		}
	}
	
	/**
	 * 通过部署id单个删除流程定义
	 */
	@Test
	public void deleteProcessDefinition(){
		//创建流程定义service
		RepositoryService repositoryService = processEngine.getRepositoryService();
		//部署id
		String deploymentId = "1901";
		
		//不带级联的删除（不常用）：只能删除没有启动流程实例和已经启动流程实例且流程实例已完成的流程定义，如有流程实例启动后正在进行中，会抛出异常：
		//com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException:
		//Cannot delete or update a parent row: a foreign key constraint fails (`activitidb`.`act_ru_execution`, 
		//CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`))
		//删除成功后，act_re_procdef的流程定义对象、act_re_deployment的部署对象、act_ge_bytearray的资源文件都会被删除，
		//但act_hi_procinst、act_hi_actinst、act_hi_taskinst记录的已完成的实例、活动、任务不会被删除。
		//删除的流程定义如果是版本号是最新的，下次部署该流程图的时候，版本号还是这个值；但如果删除的版本号不是最新的，下次部署的版本号会是最新的版本号+1。
//		repositoryService.deleteDeployment(deploymentId);
		
		//级联删除（常用）：可以强制删除所有流程定义对象（即使已启动流程实例且未完成）；
		//除了act_re_procdef的流程定义对象、act_re_deployment的部署对象、act_ge_bytearray的资源文件都会被删除，
		//act_hi_procinst、act_hi_actinst、act_hi_taskinst记录的已完成的实例、活动、任务，
		//act_ru_execution、act_ru_task记录的正在进行中的执行对象、任务也会被删除
		repositoryService.deleteDeployment(deploymentId, true);
		
		System.out.println("流程定义删除成功");
	}
	
	/**
	 * 通过流程定义key值删除多个流程定义
	 */
	@Test
	public void deleteProcessDefinitionByKey(){
		//流程定义key值
		String key = "leaveProcessId2";
		
		List<ProcessDefinition> list = processEngine.getRepositoryService()
											.createProcessDefinitionQuery()
											.processDefinitionKey(key)
											.list();
		
		if(null != list && 0 < list.size()){
			for(ProcessDefinition pd : list){
				processEngine.getRepositoryService().deleteDeployment(pd.getDeploymentId(), true);
			}
			System.out.println("删除成功");
		}
	}
	
	/**
	 * 查看流程图
	 */
	@Test
	public void viewPic(){
		//部署id
		String deploymentId = "2701";
		
		//根据部署id获取流程图资源全局定义名（bpmn+png）
		//由于它是读的act_ge_bytearray资源表的资源全局定义名（以及之后读的输入流也是读的该表的二进制文件流），
		//即使是用zip包部署的，在资源表中也是有这两个格式的文件的，读流也是读各资源文件的二进制文件流，不会去读zip包的流
		List<String> list = processEngine.getRepositoryService().getDeploymentResourceNames(deploymentId);
		
		//定义图片资源的全局定义名
		String resourceName = "";
		if(null != list && 0 < list.size()){
			for(String name : list){
				if(0 <= name.indexOf(".png")){
					resourceName = name;
				}
			}
		}
		
		//获取图片输入流
		InputStream in = processEngine.getRepositoryService().getResourceAsStream(deploymentId, resourceName);
		//在D盘下新建一个与该图片相同全局定义名的文件
		File file = new File("D:/" + resourceName);
		//将输入流的图片写入到新建的文件中,FileUtils需要引入依赖（或者引入commons-fileupload-1.3.1.jar和commons-io-2.2.jar，
		//activiti安装包activiti-5.22.0.zip解压后的activiti-5.22.0\wars\activiti-rest.war中WEB-INF\lib目录下有）：
		//<dependency>
	    //	<groupId>commons-fileupload</groupId>
	    //	<artifactId>commons-fileupload</artifactId>
	    //	<version>1.3.1</version>
    	//</dependency>
		try {
			FileUtils.copyInputStreamToFile(in, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询最新版本的流程定义对象（根据key值分组，每组取version最大的流程定义）
	 */
	@Test
	public void findLastVersionProcessDefinition(){
		//查询所有的流程定义对象，并按版本号升序排列
		List<ProcessDefinition> list = processEngine.getRepositoryService()
													.createProcessDefinitionQuery()
													.orderByProcessDefinitionVersion().asc()
													.list();
		
		//由于list是排过序的，这里使用LinkedHashMap来封装，
		//把list中的流程定义对象逐个put到map中，相同key值的最后put进来的流程定义对象会成为该key值对应的唯一流程定义对象，
		//又由于list是升序排列，所以最后各key值对应的流程定义对象都是version最大的流程定义对象
		Map<String, ProcessDefinition> map = new LinkedHashMap<String, ProcessDefinition>();
		if(null != list && 0 < list.size()){
			for(ProcessDefinition pd : list){
				map.put(pd.getKey(), pd);
			}
		}
		
		//将map的流程定义对象封装到list集合中再遍历
		List<ProcessDefinition> pdList = new ArrayList<ProcessDefinition>(map.values());
		if(null != pdList && 0 < pdList.size()){
			for(ProcessDefinition pd : pdList){
				System.out.println("流程定义id：" + pd.getId());
				System.out.println("流程定义名称：" + pd.getName());
				System.out.println("流程定义key：" + pd.getKey());
				System.out.println("流程定义版本：" + pd.getVersion());
				System.out.println("资源bpmn全局定义名：" + pd.getResourceName());
				System.out.println("资源png全局定义名：" + pd.getDiagramResourceName());
				System.out.println("部署对象id：" + pd.getDeploymentId());
				System.out.println("#########################################");
			}
		}
	}
	
	/**
	 * 设置变量
	 */
	@Test
	public void setVariable(){
		RuntimeService runtimeService = processEngine.getRuntimeService();
		//参数依次是:执行对象id，变量名，变量值
		runtimeService.setVariable("13301", "runVar1", 1);
		runtimeService.setVariableLocal("13301", "runVar2", 2);
		
		TaskService taskService = processEngine.getTaskService();
		//参数依次是:任务对象id，变量名，变量值
		taskService.setVariable("13304", "taskVar1", 1);
		taskService.setVariableLocal("13304", "taskVar2", 2);
		
		//runtimeService.setVariable的复数形式：一次设置多个变量
		//其他三种设置变量方式的复数形式可类比
		Map<String, Object> var = new HashMap<String, Object>();
		var.put("runVar3", 3);
		var.put("runVar4", 4);
		//参数依次是:执行对象id，由多个变量名、变量值组成的Map对象
		runtimeService.setVariables("13301",var);
		
		//当设置变量是serializable类型（即javabean），那么，该javabean必须实现序列化，否则会抛出异常
		//javabean实现序列化后是不能修改的（如加字段），如果修改了就无法获取到变量了，因为修改之前这个javabean是序列号a，存入变量，
		//修改之后，这个javabean是序列号b，获取的时候仍然以序列号a去反序列化获取，就找不到了。解决方案是让javabean的序列号固定下来，
		//javabean就不会用自动生成的javabean的，修改前后都是同一个序列号，修改后也能找到修改前存入的变量了
		Person p = new Person();
		p.setId(12);
		p.setName("小红");
		runtimeService.setVariable("14001", "人员信息", p);
	}
	
	/**
	 * 获取变量
	 */
	@Test
	public void getVarible(){
		RuntimeService runtimeService = processEngine.getRuntimeService();
		TaskService taskService = processEngine.getTaskService();
		
		//获取单个变量都是返回Object类型，在返回值类型确定的情况下，应该强转
		System.out.println(runtimeService.getVariable("13301", "runVar1"));//1
		System.out.println(runtimeService.getVariable("13301", "runVar2"));//2
		System.out.println(runtimeService.getVariable("13301", "taskVar1"));//1
		System.out.println(runtimeService.getVariable("13301", "taskVar2"));//null
		System.out.println(taskService.getVariable("13505", "runVar1"));//1
		System.out.println(taskService.getVariable("13505", "runVar2"));//2
		System.out.println(taskService.getVariable("13505", "taskVar1"));//1
		System.out.println(taskService.getVariable("13505", "taskVar2"));//2
		System.out.println("#########################");
		System.out.println(runtimeService.getVariableLocal("13301", "runVar1"));//1
		System.out.println(runtimeService.getVariableLocal("13301", "runVar2"));//2
		System.out.println(runtimeService.getVariableLocal("13301", "taskVar1"));//1
		System.out.println(runtimeService.getVariableLocal("13301", "taskVar2"));//null
		System.out.println(taskService.getVariableLocal("13505", "runVar1"));//null
		System.out.println(taskService.getVariableLocal("13505", "runVar2"));//null		
		System.out.println(taskService.getVariableLocal("13505", "taskVar1"));//null
		System.out.println(taskService.getVariableLocal("13505", "taskVar2"));//2
		System.out.println("#########################");
		//以下查询结果排除了runVar3、runVar4
		System.out.println(runtimeService.getVariables("13301"));//{runVar2=2, runVar1=1, taskVar1=1}
		System.out.println(runtimeService.getVariablesLocal("13301"));//{runVar2=2, runVar1=1, taskVar1=1}
		System.out.println(taskService.getVariables("13505"));//{runVar2=2, taskVar2=2, runVar1=1, taskVar1=1}
		System.out.println(taskService.getVariablesLocal("13505"));//{taskVar2=2}
		
		//用key值组成的集合获取多个指定变量的值，这里只拿一种api做示范，其他api类比
		List<String> keyList = new ArrayList<String>();
		keyList.add("runVar1");
		keyList.add("runVar2");
		keyList.add("taskVar1");
		keyList.add("taskVar2");
		System.out.println(taskService.getVariables("13505", keyList));//{runVar1=1, runVar2=2, taskVar1=1, taskVar2=2}
	
		//获取serializable类型的变量（即javabean类型）
		//变量值记录在act_ge_bytearray的bytearray_id字段上，但这只是一个索引，根据该索引，到act_ge_bytearray表中找，便可找到真正的值——一个二进制文件。
		Person p = (Person) runtimeService.getVariable("14001", "人员信息");
		System.out.println(p.getId() + "	" + p.getName());
	}
	
	/**
	 * 查询历史变量
	 */
	@Test
	public void findHistoryProcessVariable(){
		//创建管理历史数据的service
		HistoryService historyService = processEngine.getHistoryService();
		
		//创建一个历史变量查询对象，一个HistoricVariableInstanceQuery对象只能调用一次查询api，多次调用会报错，应生成多个HistoricVariableInstanceQuery对象去查询
		HistoricVariableInstanceQuery hviq = historyService.createHistoricVariableInstanceQuery();
		
		//根据历史变量名称（key值）查询历史变量信息，返回一个集合对象
		List<HistoricVariableInstance> list = hviq.variableName("runVar1").list();
		
		if(null != list && 0 < list.size()){
			for(HistoricVariableInstance hvi : list){
				System.out.println(hvi.getId() + "	" + hvi.getProcessInstanceId() + "	" + hvi.getVariableName() + "	" + hvi.getVariableTypeName() + "	" + hvi.getValue());
				System.out.println("#########################");
			}
		}
	}
}
