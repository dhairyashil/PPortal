package com.mnt.module.appointment.service;

import java.util.List;

import com.mnt.model.Patient;
import com.mnt.pojo.User;
import com.mnt.vm.ApplicationDataVM;

public interface UserDataService {
	public List<ApplicationDataVM> getAllDoctorsData();
	public Patient validatePatientUser(User user);
	public void updateUserPassword(String code,String newPassword);
}
