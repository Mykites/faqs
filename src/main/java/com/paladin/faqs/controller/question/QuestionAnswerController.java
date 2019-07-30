package com.paladin.faqs.controller.question;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.paladin.framework.core.GlobalProperties;

/**   
 * @author MyKite
 * @version 2019年7月30日 上午11:08:51 
 */
@Controller
@RequestMapping("/" + GlobalProperties.project+"/question")
public class QuestionAnswerController {

    @GetMapping("/index")
    public String index(){
	return "/" + GlobalProperties.project + "/question/question_answer_index";
    }
}
