package pt.notub.email.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class ServicoEnvioEmail {

    private static final Logger logger = LoggerFactory.getLogger(ServicoEnvioEmail.class);
    private static final String REMETENTE = "notub@noreply.com";

    private final JavaMailSender mailSender;

    public ServicoEnvioEmail(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarBoasVindas(String emailDestino, String primeiroNome, String ultimoNome) {
        String nome = (primeiroNome != null ? primeiroNome : "Utilizador");
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(REMETENTE);
            helper.setTo(emailDestino);
            helper.setSubject("Bem-vindo ao NoTUB!");

            String html = """
            <div style="max-width:480px;margin:0 auto;font-family:'Segoe UI',Arial,sans-serif;background:#f8f9fa;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#0e2d24 0%,#028e5c 100%);padding:32px 24px;text-align:center;">
                <img src="cid:logoNotub" alt="NoTUB" style="height:48px;margin-bottom:12px;"/>
                <h1 style="color:#ffffff;font-size:22px;font-weight:800;margin:0;">Bem-vindo ao NoTUB!</h1>
              </div>
              <div style="padding:28px 24px;">
                <p style="font-size:16px;color:#212529;margin:0 0 16px 0;">Ol&aacute; <strong>""" + nome + """
                </strong>,</p>
                <p style="font-size:14px;color:#495057;line-height:1.6;margin:0 0 20px 0;">
                  A sua conta foi criada com sucesso! Est&aacute; pronto para come&ccedil;ar a viajar connosco.
                </p>
                <div style="background:#ffffff;border:1px solid #e9ecef;border-radius:10px;padding:20px;margin:0 0 20px 0;">
                  <p style="font-size:14px;color:#028e5c;font-weight:700;margin:0 0 12px 0;">O que pode fazer com o NoTUB:</p>
                  <table style="width:100%;border-collapse:collapse;">
                    <tr>
                      <td style="padding:6px 0;font-size:13px;color:#495057;">
                        <span style="color:#028e5c;">&#10003;</span> Comprar bilhetes e passes
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:6px 0;font-size:13px;color:#495057;">
                        <span style="color:#028e5c;">&#10003;</span> Planear as suas viagens
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:6px 0;font-size:13px;color:#495057;">
                        <span style="color:#028e5c;">&#10003;</span> Acumular e resgatar pontos
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:6px 0;font-size:13px;color:#495057;">
                        <span style="color:#028e5c;">&#10003;</span> Consultar hor&aacute;rios em tempo real
                      </td>
                    </tr>
                  </table>
                </div>
                <div style="text-align:center;">
                  <a href="https://localhost" style="display:inline-block;background:linear-gradient(135deg,#028e5c 0%,#01bc74 100%);color:#ffffff;text-decoration:none;padding:12px 32px;border-radius:8px;font-size:14px;font-weight:700;">
                    Comece a Explorar
                  </a>
                </div>
              </div>
              <div style="background:#0e2d24;padding:20px 24px;text-align:center;">
                <p style="font-size:11px;color:rgba(255,255,255,0.6);margin:0;">
                  Equipa NoTUB &bull; Transportes (NO) Urbanos de Braga
                </p>
                <p style="font-size:10px;color:rgba(255,255,255,0.4);margin:6px 0 0 0;">
                  Este email foi enviado automaticamente. N&atilde;o responda a esta mensagem.
                </p>
              </div>
            </div>
            """;

            helper.setText(html, true);

            ClassPathResource logoResource = new ClassPathResource("static/logo.png");
            try (InputStream is = logoResource.getInputStream()) {
                byte[] logoBytes = is.readAllBytes();
                helper.addInline("logoNotub", new ByteArrayDataSource(logoBytes, "image/png"));
            }

            mailSender.send(mimeMessage);
            logger.info("Email de boas-vindas HTML enviado para: {}", emailDestino);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de boas-vindas: {}", e.getMessage());
        }
    }

    public void enviarRecuperacaoPassword(String emailDestino, String primeiroNome, String urlRecuperacao) {
        String nome = primeiroNome != null ? primeiroNome : "Utilizador";
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(REMETENTE);
            helper.setTo(emailDestino);
            helper.setSubject("NoTUB - Recuperacao de Palavra-passe");

            String html = """
            <div style="max-width:480px;margin:0 auto;font-family:'Segoe UI',Arial,sans-serif;background:#f8f9fa;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#0e2d24 0%,#028e5c 100%);padding:32px 24px;text-align:center;">
                <img src="cid:logoNotub" alt="NoTUB" style="height:48px;margin-bottom:12px;"/>
                <h1 style="color:#ffffff;font-size:20px;font-weight:800;margin:0;">Recuperar Palavra-passe</h1>
              </div>
              <div style="padding:28px 24px;">
                <p style="font-size:16px;color:#212529;margin:0 0 16px 0;">Ol&aacute; <strong>""" + nome + """
                </strong>,</p>
                <p style="font-size:14px;color:#495057;line-height:1.6;margin:0 0 20px 0;">
                  Recebemos um pedido para redefinir a sua palavra-passe. Clique no bot&atilde;o abaixo para criar uma nova:
                </p>
                <div style="text-align:center;margin:0 0 20px 0;">
                  <a href=\"""" + urlRecuperacao + """
                  " style="display:inline-block;background:linear-gradient(135deg,#028e5c 0%,#01bc74 100%);color:#ffffff;text-decoration:none;padding:12px 32px;border-radius:8px;font-size:14px;font-weight:700;">
                    Redefinir Palavra-passe
                  </a>
                </div>
                <p style="font-size:12px;color:#868e96;line-height:1.5;margin:0 0 8px 0;">
                  Este link &eacute; v&aacute;lido durante <strong>30 minutos</strong>.
                </p>
                <p style="font-size:12px;color:#868e96;line-height:1.5;margin:0;">
                  Se n&atilde;o solicitou esta altera&ccedil;&atilde;o, pode ignorar este email com seguran&ccedil;a.
                </p>
              </div>
              <div style="background:#0e2d24;padding:20px 24px;text-align:center;">
                <p style="font-size:11px;color:rgba(255,255,255,0.6);margin:0;">
                  Equipa NoTUB &bull; Transportes (NO) Urbanos de Braga
                </p>
                <p style="font-size:10px;color:rgba(255,255,255,0.4);margin:6px 0 0 0;">
                  Este email foi enviado automaticamente. N&atilde;o responda a esta mensagem.
                </p>
              </div>
            </div>
            """;

            helper.setText(html, true);

            ClassPathResource logoResource = new ClassPathResource("static/logo.png");
            try (InputStream is = logoResource.getInputStream()) {
                byte[] logoBytes = is.readAllBytes();
                helper.addInline("logoNotub", new ByteArrayDataSource(logoBytes, "image/png"));
            }

            mailSender.send(mimeMessage);
            logger.info("Email de recuperacao de password HTML enviado para: {}", emailDestino);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de recuperacao: {}", e.getMessage());
        }
    }
}
