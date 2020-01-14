package com.ibm.nscontainercrush.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.RequestTooLargeException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.nscontainercrush.config.VisualToTextConfiguration;
import com.ibm.nscontainercrush.dto.SkuItem;
import com.ibm.nscontainercrush.util.ContainerCrushUtil;
import com.ibm.watson.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.visual_recognition.v3.model.ClassifiedImage;
import com.ibm.watson.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.visual_recognition.v3.model.ClassifierResult;
import com.ibm.watson.visual_recognition.v3.model.ClassifyOptions;

@Service
public class VisualToTextService {
	
	private static final Logger logger = LoggerFactory.getLogger(VisualToTextService.class);
	
	@Autowired
	private VisualToTextConfiguration vttConfig;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private ProductCatalogueService productCatalogueService;
	
	/**
	 * This method is used to retrieve items based on results retrieved from watson visual to text service
	 * @param bytes
	 * @return
	 * @throws Exception
	 */
	public List<SkuItem> retrieveItemsUsingVisualToTextConversion (byte[] bytes) throws Exception {
		List<String> extractedWords = convertVisualToText(bytes);
		List<String> finalWordList = ContainerCrushUtil.getFilteredWords(extractedWords, env.getProperty("definedKeywords"));
		
		return productCatalogueService.findItemsByTextArray(finalWordList);
	}
	
	private List<String> convertVisualToText(byte[] bytes) throws Exception {
		
		try {
			ClassifiedImages result = invokeWatsonVisualToTextProcess(bytes);
			return extractWordsFromWatsonResult(result);
		} catch (Exception e) {
			logger.error("Error occurred while processing speech to text : " + e.getMessage());
			throw new Exception("Error occurred while processing speech to text : " + e.getMessage());
		}
	}
	
	private ClassifiedImages invokeWatsonVisualToTextProcess(byte[] bytes) {
		IamAuthenticator authenticator = new IamAuthenticator(vttConfig.getAuthenticatorKey());
		VisualRecognition visualRecognition = new VisualRecognition(vttConfig.getVersionDate(), authenticator);
		visualRecognition.setServiceUrl(vttConfig.getServiceUrl());
		ClassifiedImages result = null;
		List<String> strList = null;
		try {
			InputStream imagesStream = new ByteArrayInputStream(bytes);
			ClassifyOptions classifyOptions = new ClassifyOptions.Builder().imagesFile(imagesStream)
					.imagesFilename("fruitbowl.jpg").imagesFileContentType("image/jpg")
					.classifierIds(Arrays.asList("Genderdetection_714036171")).build();

			result = visualRecognition.classify(classifyOptions).execute().getResult();

		} catch (NotFoundException e) {
			logger.error("Image file not found while processing watson visual to text service " + e.getStatusCode() + ": " + e.getMessage());
		} catch (RequestTooLargeException e) {
			logger.error("Image file too large while processing watson visual to text service " + e.getStatusCode() + ": " + e.getMessage());
		} catch (ServiceResponseException e) {
			// Base class for all exceptions caused by error responses from the service
			logger.error("Error received from watson visual to text service " + e.getStatusCode() + ": " + e.getMessage());
		}

		return result;
	}
	
	private List<String> extractWordsFromWatsonResult(ClassifiedImages result) {
		List<String> extractedWordList = null;
		if (result != null && result.getImages() !=null && !result.getImages().isEmpty()) {
			extractedWordList = new ArrayList<>();
			for (ClassifiedImage clImage : result.getImages()) {
				if (clImage != null && clImage.getClassifiers() != null && !clImage.getClassifiers().isEmpty()) {
					for (ClassifierResult clrResult:clImage.getClassifiers()) {
						if (clrResult.getClasses() != null && !clrResult.getClasses().isEmpty()) {
							List<ClassResult> classResults = clrResult.getClasses();
							//classResults.forEach(classResult -> extractedWordList.add(classResult.getXClass()));
							for (ClassResult clResult : clrResult.getClasses()) {
								if (clResult.getXClass() != null) {
									extractedWordList.add(clResult.getXClass());
								}
							}
						}
					}
				}
			}
		}
		return extractedWordList;
	}

}
