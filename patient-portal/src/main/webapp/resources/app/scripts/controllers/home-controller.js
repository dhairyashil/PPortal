'use strict';

var app = angular.module('home-app');

///////////////////////// Search Service Start //////////////////////////////////

app.controller('HomeController',function($scope, $location, $filter, AppointmentSlotService, MetaDataService){
	
	$scope.symptomFormArray = [];
	$scope.currentSymptomFormIndex = 0;
	
	$scope.levelOneOption = [
	                         {text:"Head" , value:4, sub:[
	                                                      	{text:"Choose" , value:null},
	                                                        {text:"Scalp" , value:7},
	                                                        {text:"Fore Head" , value:8},
	                                                        {text:"Right Eye" , value:9},
	                                                        {text:"Left Eye" , value:10},
	                                                        {text:"Nose" , value:6},
	                                                        {text:"Right Ear" , value:11},
	                                                        {text:"Left Ear" , value:12},
	                                                        {text:"Mouth" , value:5},
	                                                        {text:"Jaws" , value:13},
	                                                        {text:"Chin" , value:14},
	                                                        {text:"Right Face" , value:15},
	                                                        {text:"Left Face" , value:16}]},
	                         {text:"Neck(Front)" , value:20, sub:[
	                                                         {text:"Throat" , value:18},
	                                                         {text:"Neck" , value:19}]},
	                         {text:"Chest" , value:21, sub:[
	                                                         {text:"Left Chest" , value:22},
	                                                         {text:"Right Chest" , value:23}]},
	                         {text:"Right Upper Arm" , value:1, sub:[{text:"" , value:""}]},
	                         {text:"Left Upper  Arm" , value:30, sub:[{text:"" , value:""}]},
	                         {text:"Abdomen" , value:25, sub:[{text:"" , value:""}]},
	                         {text:"Right Lower Arm" , value:21, sub:[{text:"" , value:""}]},
	                         {text:"Left Lower Arm" , value:22, sub:[{text:"" , value:""}]},
	                         {text:"Pelvis" , value:23, sub:[{text:"" , value:""}]},
	                         {text:"Right Thigh Area" , value:24, sub:[{text:"" , value:""}]},
	                         {text:"Left Thigh Area" , value:31, sub:[{text:"" , value:""}]},
	                         {text:"Right Calf Area" , value:32, sub:[{text:"" , value:""}]},
	                         {text:"Left Calf Area" , value:28, sub:[{text:"" , value:""}]}
	                         ];
	
	
	$scope.symptomForm = {
			levelOneArea : 4,
			levelTwoArea : 1,
			painArea: 1,
			whenThisHappen : 1
	};
	
	// this DS will be part of POST Data
	$scope.symptomFormArray.push($scope.symptomForm);
	
	$scope.updateLevelTwo = function() {
		angular.forEach($scope.levelOneOption, function(item){
	        if(item.value == $scope.symptomFormArray[$scope.currentSymptomFormIndex].levelOneArea) {
	        	$scope.levelTwoOption = item.sub;
	        }
	      });
		$scope.setLevelTwo($scope.levelTwoOption[0].value);
		$scope.$apply;
	};
	
	$scope.removeThisSymptom = function(index) {
		$scope.symptomFormArray.splice(index, 1);
		$scope.currentSymptomFormIndex = 0;
	};
	
	$scope.setLevelOne = function(id) {
		$scope.symptomFormArray[$scope.currentSymptomFormIndex].levelOneArea = id;
		$scope.updateLevelTwo();
		
	};
	
	$scope.setLevelTwo = function(id) {
		$scope.symptomFormArray[$scope.currentSymptomFormIndex].levelTwoArea = id;
		getMetadata(id);
	};
	$scope.metadata = {
			painArea : [] 
	};
	function getMetadata(subid){
		MetaDataService.GetMetaDataByLocation.get({
						location:$scope.symptomFormArray[$scope.currentSymptomFormIndex].levelOneArea,
						sublocation: subid}, function(data){
			
						$scope.symptomFormArray[$scope.currentSymptomFormIndex].metadata = data;
						$scope.$apply;
		});
	}
	
	$scope.addOtherSymptom = function() {
		$scope.symptomFormArray.push({});
		$scope.currentSymptomFormIndex++;
		$("#imgZoomOutButton").click();
		console.log($scope.symptomFormArray);
	};
	
	$scope.setcurrentSymptomFormIndex = function(index) {
		$scope.currentSymptomFormIndex = index;
		$scope.$apply;
		console.log($scope.symptomFormArray[$scope.currentSymptomFormIndex]);
	};
	
	$scope.primaryDoctorOption = [{text:"Dr Gary", value:1},
	                              {text:'Dr James', value:2},
	                              {text:'Dr Jagbir', value:3},
	                              {text:'Dr Amit', value:4}];
	
	
	$scope.specialtiesOption = [  {text:"Pediatrician", value:1},
	                              {text:'Cardiology', value:2},
	                              {text:'Neurology', value:3},
	                              {text:'Radiology', value:4}];
	
	$scope.doctorsOption = [{text:"Dr Gary", value:1},
	                        {text:'Dr James', value:2},
	                        {text:'Dr Jagbir', value:3},
	                        {text:'Dr Amit', value:4}];
	
	
	
	// this DS will be part of POST Data
	$scope.appointmentForm = {
			appointmentDate : '',
			slot : [],
			primaryDoctor: 1,
			specialty : 1,
			doctor : 1
	}; 
	
	$scope.slots = [];
	$scope.checkAvailability = function() {
		AppointmentSlotService.GetAppointmentSlots.get({date:$filter('date')($scope.appointmentForm.appointmentDate,'yyyyMMdd')},function(data) {
			$scope.slots = data;
		});
	};
	
	var isMutipleSlotSelectionAllowed = true; 
	$scope.markAppoitment = function(slot) {
		
		if(isMutipleSlotSelectionAllowed == false) {
			if(slot.status == 'REQUESTED') {
				slot.status = 'AVAILABLE';
			} else if(slot.status == 'AVAILABLE') {
				slot.status = 'REQUESTED';
			}
		} else {
			angular.forEach($scope.slots,  function(_slot) {
				if(_slot.status == 'REQUESTED') {
					_slot.status = 'AVAILABLE';
				}
			});
			slot.status = 'REQUESTED';
		}
	};
	
	
	$scope.bookAppoitment = function () {
		$scope.appointmentForm.slot = $filter('filter')($scope.slots,{status:"REQUESTED"});
		$scope.appointmentForm.appointmentWith = 1; // hardcodded for now
		angular.foreach($scope.symptomFormArray, function(data){
			data.metadata = ''; 
		});;
		AppointmentSlotService.BookAppointmentSlot.save({
			appointment : $scope.appointmentForm,
			symptoms : $scope.symptomFormArray
		});
	};
	
	
	$scope.step = 1;
	
	$scope.doneSymptomSelection = function () {
		$scope.step = 2;
		
	};
	
	$scope.gotoSymptomSelection = function () {
		$scope.step = 1;
	};
    
	
	
	
	
	
});
