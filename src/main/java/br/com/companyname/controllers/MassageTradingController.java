package br.com.companyname.controllers;

import br.com.companyname.model.Login;
import br.com.companyname.model.Massage;
import br.com.companyname.service.MassageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@SessionAttributes("login")
public class MassageTradingController {

    @Autowired
    private MassageService massageService;

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/massageForm")
    public String massageForm(Map<String, Object> model) {
        model.put("massage", new Massage());
        model.put("professionals", Massage.Professional.values());
        return "massageForm";
    }

    @GetMapping("/massage/exchange/{massageId}/{exchange}")
    public String exchangeMassage(@PathVariable String massageId, @PathVariable("exchange") Boolean exchange, @ModelAttribute final Login login, final ModelMap model){
        Massage massage = massageService.findMassageById(massageId);
        massage.setVisible(exchange);
        massageService.save(massage);

        model.put("username", login.getEmail());
        model.put("myMassages", massageService.findAllByLogin(login));
        model.put("massages", massageService.list());

        return "welcome";
    }

    /**
     * Refresh the table that contains the list of massages
     * @param login
     * @param model
     * @return
     */
    @GetMapping("/ativeExchangeMassages")
    public String exchangeMassage(@ModelAttribute final Login login, final ModelMap model){
        model.put("username", login.getEmail());
        model.put("myMassages", massageService.findAllByLogin(login));
        model.put("massages", massageService.list());
        return "welcome";
    }

    /**
     * Exchange the massage with another the table that contains the list of massages
     * @param login
     * @param model
     * @return
     */
    @GetMapping("/exchangeMessage/{massageToExchangeId}")
    public String exchangeMassage(@PathVariable("massageToExchangeId") String messageToExchangeId, @ModelAttribute final Login login, final ModelMap model){

        model.put("username", login.getEmail());

        //TODO - tem que bsucar todas e eventualmente o usuário escolher
        Massage myMassage = massageService.findUserMassage(login);

        Massage massageToExchange = massageService.findMassageById(messageToExchangeId);

        //Change the owner of massage.
        myMassage.setLogin(massageToExchange.getLogin());

        //I'm now the owner of the other massage.
        massageToExchange.setLogin(login);

        //turn both messages invisible(not negociate)
        myMassage.setVisible(Boolean.FALSE);
        massageToExchange.setVisible(Boolean.FALSE);

        massageService.save(myMassage);
        massageService.save(massageToExchange);

        //send email to both exchanged users
        sendConfirmationEmails(myMassage, massageToExchange);

        model.put("myMassages", massageService.findAllByLogin(login));
        model.put("massages", massageService.list());
        model.put("success", "Você trocou uma massagem com : " + myMassage.getLogin().getEmail() + "\n Um email foi enviado a vocês dois.");

        return "welcome";
    }

    /**
     * Set a no-owner massage to a login
     * @param login
     * @param model
     * @return
     */
    @GetMapping("/getDonatedMassage/{massageToDonateId}")
    public String donateMassage(@PathVariable("massageToDonateId") String massageToDonateId, @ModelAttribute final Login login, final ModelMap model){
        Massage massage = massageService.findMassageById(massageToDonateId);
        massage.setLogin(login);
        massage.setVisible(Boolean.FALSE);

        massageService.save(massage);

        model.put("username", login.getEmail());
        model.put("myMassages", massageService.findAllByLogin(login));
        model.put("massages", massageService.list());
        model.put("success", "Massagem reservada com sucesso");
        return "welcome";
    }



    private void sendConfirmationEmails(Massage myMassage, Massage massageToExchange) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(myMassage.getLogin().getEmail());
        message.setSubject("Massage trader - Troca de massagem");
        message.setText(String.format("Seu novo horário é : %s com %s. \nVocê trocou sua massagem com : %s", myMassage.getTime(), myMassage.getProfessional().toString() , massageToExchange.getLogin().getEmail()));
        mailSender.send(message);

        message = new SimpleMailMessage();
        message.setTo(massageToExchange.getLogin().getEmail());
        message.setSubject("Massage trader - Troca de massagem");
        message.setText(String.format("Seu novo horário é : %s com %s. \nVocê trocou sua massagem com : %s", massageToExchange.getTime(), massageToExchange.getProfessional().toString(), myMassage.getLogin().getEmail()));
        mailSender.send(message);
    }


}