package com.buhao.bean;

import java.io.Serializable;

public class Person implements Serializable{
	/**
	 * 将序列号固定下来
	 */
	private static final long serialVersionUID = 2999012126592748462L;
	
	private Integer id;
	private String name;
	private String education;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEducation() {
		return education;
	}
	public void setEducation(String education) {
		this.education = education;
	}
}
