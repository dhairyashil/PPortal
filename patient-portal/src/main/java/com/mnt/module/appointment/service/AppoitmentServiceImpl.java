package com.mnt.module.appointment.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Range;
import com.mnt.model.Appointment;
import com.mnt.model.AppointmentDocument;
import com.mnt.model.AppointmentDocument.SymptomDocument;
import com.mnt.model.BodyLocation;
import com.mnt.model.Doctors;
import com.mnt.model.KeyValue;
import com.mnt.model.PainArea;
import com.mnt.model.Patient;
import com.mnt.model.RelievedBy;
import com.mnt.model.WhenThisHappen;
import com.mnt.model.WorsedBy;
import com.mnt.module.appointment.SlotStatus;
import com.mnt.module.appointment.TimeConverter;
import com.mnt.module.appointment.data.AppointmentRequest;
import com.mnt.module.appointment.data.AppointmentSlotViewModel;
import com.mnt.module.appointment.data.SymptomReported;
import com.mnt.module.appointment.repository.AppointmentDataStore;
import com.mnt.vm.AppointmentVM;

@Service
public class AppoitmentServiceImpl implements AppoitmentService {
	
	static int minInAday = 1440;//60*24;
	
	@Autowired
	@Qualifier("appointmentDataMongoStore")
	private AppointmentDataStore appointmentDataStore; 
	
	@Override
	public Set<AppointmentSlotViewModel> getAppointmentSlots(Long appointmentOf_Id,String appointmentOf_Type, Calendar day, int perSlot) {
		List<Appointment> appointments = Appointment.getAppointmentSlots(appointmentOf_Id, appointmentOf_Type, day);
		return buildAppointmentSlots(appointments, perSlot);
	}

	private Set<AppointmentSlotViewModel> buildAppointmentSlots(
			List<Appointment> appointments, int perSlot) {
		Set<AppointmentSlotViewModel> slots = new TreeSet<AppointmentSlotViewModel>();
		int noOfSlotsInADay = minInAday / perSlot;
		
		for (int startTime = 0,  slot = 1 ; slot <= noOfSlotsInADay ; slot++, startTime = slot * perSlot) {
			boolean isSlotTaken = false;
			AppointmentSlotViewModel appointmentSlot;
			for (Appointment appointment : appointments) {
				 try {
					 if(appointment.startMin > appointment.endMin) {
						 Range.open(startTime, startTime + perSlot).intersection(Range.open(appointment.startMin, minInAday + appointment.endMin));
						 isSlotTaken = true;
					 } else {
						 Range.open(startTime, startTime + perSlot).intersection(Range.open(appointment.startMin, appointment.endMin));
						 isSlotTaken = true;
					 }
				 } catch(Exception exception) {
					 // Slot is not taken.
					 isSlotTaken = false;
				 }
				 
				 if(isSlotTaken) {
					 break;
				 }
			}
			
			if(isSlotTaken) {
				 appointmentSlot = new AppointmentSlotViewModel(startTime, startTime + perSlot, SlotStatus.BOOKED );
			} else {
				 appointmentSlot = new AppointmentSlotViewModel(startTime, startTime + perSlot, SlotStatus.AVAILABLE );
			}
			
			slots.add(appointmentSlot);
			
		}
		return slots;
	}

	@Override
	public String bookAppointment(AppointmentRequest request) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(request.bookingRequest.appointmentDate);
		int	startTime = TimeConverter.convertDayTimeToElapsedMin(request.bookingRequest.slots[0].startTime);
		int	endTime = TimeConverter.convertDayTimeToElapsedMin(request.bookingRequest.slots[0].endTime);
		
		Long appointmentId = Appointment.makeAppointmentOfXForADayY
		(request.bookingRequest.loggedUserId,"User",request.bookingRequest.appointmentWith,"Doctor",startTime, endTime, cal);
		
		
		appointmentDataStore.saveAppointmentAsDocument(convertAppointmentRequestToAppointmentDocument(request,appointmentId));
		return appointmentId.toString();
	}
	
	private AppointmentDocument convertAppointmentRequestToAppointmentDocument(AppointmentRequest request, Long appoitmentId ) {
		AppointmentDocument appointmentDocument = new AppointmentDocument();
		appointmentDocument.appointmentDate = request.bookingRequest.appointmentDate;
		appointmentDocument.appointmentWith = new KeyValue<Long, String>(request.bookingRequest.appointmentWith,
				/*Doctors.getFullNameById(request.bookingRequest.appointmentWith)*/request.bookingRequest.selectedDoctorText);
		appointmentDocument.appoitmentId = appoitmentId;
		//TODO
		appointmentDocument.appointmentOf = new KeyValue<Long, String>(request.bookingRequest.loggedUserId /*appointmentWith*/,
				/*Doctors.getFullNameById(request.bookingRequest.appointmentWith)*/ request.bookingRequest.loggedUserName);
		
		appointmentDocument.doctor = new KeyValue<Long, String>(request.bookingRequest.doctor,
				/*Doctors.getFullNameById(request.bookingRequest.doctor)*/ request.bookingRequest.selectedDoctorText);
		appointmentDocument.primaryDoctor = new KeyValue<Long, String>(request.bookingRequest.primaryDoctor,
				/*Doctors.getFullNameById(request.bookingRequest.primaryDoctor)*/ request.bookingRequest.selectedprimaryDoctorText);
		appointmentDocument.specialty = new KeyValue<Long, String>(request.bookingRequest.specialty, request.bookingRequest.selectedSpecialtyText);
		/*request.bookingRequest.appointmentWith,
				Doctors.getFullNameById(request.bookingRequest.appointmentWith));*/
		appointmentDocument.endTime = request.bookingRequest.slots[0].endTime;
		appointmentDocument.startTime = request.bookingRequest.slots[0].startTime;
		
		List<SymptomDocument> documentList = new ArrayList<>();
		for(SymptomReported sr : request.symptom) {
			SymptomDocument document = new SymptomDocument();
			document.levelOne = new KeyValue<Long, String>(sr.levelOneArea, sr.selectedLevelOneText /*BodyLocation.getNameById(sr.levelOneArea)*/);
			document.levelTwo = new KeyValue<Long, String>(sr.levelTwoArea, sr.selectedLevelTwoText /*BodyLocation.getNameById(sr.levelTwoArea)*/);
			document.painArea = new KeyValue<Long, String>(sr.painArea,  sr.selectedPainAreaText /*PainArea.getDescriptionById(sr.painArea)*/);
			document.painLasts = sr.painLasts;
			document.painLastsUnit = sr.painLastsUnit;
			document.relievedBy = new KeyValue<Long, String>(sr.relievedBy, sr.selectedRelievedByText /*RelievedBy.getDescriptionById(sr.relievedBy)*/);;
			document.whenThisHappen = new KeyValue<Long, String>(sr.whenThisHappen,  sr.selectedWhenThisHappenText /*WhenThisHappen.getDescriptionById(sr.whenThisHappen)*/);
			document.worseBy = new KeyValue<Long, String>(sr.worseBy,  sr.selectedWorseByText /*WorsedBy.getDescriptionById(sr.worseBy)*/);
			documentList.add(document);
		}
		appointmentDocument.symptom = documentList; 
		
		return appointmentDocument;
	}

	@Override
	public List<AppointmentVM> getAllAppointmentsOfPatient(String patientCode) {
		Patient patient = Patient.getPatientByCode(patientCode);
		List<Appointment> appointments = Appointment.getAllAppointments(patient.getId());
		
		List<AppointmentVM> appointmentVMs = new ArrayList<>();
		for(Appointment apt : appointments) {
			AppointmentVM vm = AppointmentVM.buildVM(apt);
			appointmentVMs.add(vm);
		}
		return appointmentVMs;
	}
	
	

}
