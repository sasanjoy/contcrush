package com.ibm.nscontainercrush.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.nscontainercrush.service.SpeechToTextService;

@RestController
@RequestMapping(value = "/speechToText")
public class SpeechToTextController {
	
	@Autowired
	private SpeechToTextService sttService;
	
	@PostMapping("/convertSpeechToText")
	public List<String> convertSpeechToText(){
		return sttService.convertSpeechToText();
	}

}
