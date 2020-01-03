package com.ibm.nscontainercrush.service;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.springframework.stereotype.Service;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.Authenticator;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionAlternative;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResult;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.model.SpeechWordConfidence;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;

@Service
public class SpeechToTextService {
	
	private final double base_confidence = 0.7d;
	
	public List<String> convertSpeechToText() {
		
		try {
			SpeechRecognitionResults srResults = invokeWatsonSpeechToTextProcess();
			return extractWordsFromWatsonResult(srResults);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
		
	public SpeechRecognitionResults invokeWatsonSpeechToTextProcess() throws InterruptedException, LineUnavailableException {
		Authenticator authenticator = new IamAuthenticator("6l6sM4JJ0tc1SHc2fefR5PvvZO67WbcV95jGShAXnl_w");
		SpeechToText service = new SpeechToText(authenticator);
		SpeechRecognitionResults srResults = null;
		// Signed PCM AudioFormat with 16kHz, 16 bit sample size, mono
		int sampleRate = 16000;
		AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		if (!AudioSystem.isLineSupported(info)) {
			System.out.println("Line not supported");
			System.exit(0);
		}

		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();

		AudioInputStream audio = new AudioInputStream(line);

		RecognizeOptions options = new RecognizeOptions.Builder().audio(audio).interimResults(false).timestamps(true)
				.wordConfidence(true)
				// .inactivityTimeout(5) // use this to stop listening when the speaker pauses,
				// i.e. for 5s
				.contentType(HttpMediaType.AUDIO_RAW + ";rate=" + sampleRate).build();

		service.recognizeUsingWebSocket(options, new BaseRecognizeCallback() {
			@Override
			public void onTranscription(SpeechRecognitionResults speechResults) {
				// TODO -need to capture speechResults
				// srResults=speechResults;
				System.out.println(speechResults);
			}
			
		});

		System.out.println("Listening to your voice for the next 30s...");
		Thread.sleep(10 * 1000);

		// closing the WebSockets underlying InputStream will close the WebSocket
		// itself.
		line.stop();
		line.close();

		System.out.println("Fin.");
		return srResults;
	}
	
	/**
	 * This method is used to extract the words from speech to watson recognition API output
	 * @param srResult
	 * @return
	 */
	private List<String> extractWordsFromWatsonResult(SpeechRecognitionResults srResult) {

		List<String> extractedWordList = null;

		if (srResult != null && !srResult.getResults().isEmpty()) {
			for (SpeechRecognitionResult speechResult : srResult.getResults()) {
				if (speechResult.isXFinal()) {
					int k = 0;
					extractedWordList = new ArrayList<>();
					List<SpeechRecognitionAlternative> alternatives = speechResult.getAlternatives();
					if (alternatives != null && !alternatives.isEmpty()) {
						for (SpeechRecognitionAlternative speechRecognitionAlter : alternatives) {
							if (speechRecognitionAlter.getWordConfidence() != null
									&& !speechRecognitionAlter.getWordConfidence().isEmpty()) {
								for (SpeechWordConfidence speechWordConfidence : speechRecognitionAlter
										.getWordConfidence()) {
									if (speechWordConfidence.getConfidence() > base_confidence) {
										extractedWordList.add(speechWordConfidence.getWord());
										k++;
									}
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

