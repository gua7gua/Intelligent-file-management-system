package com.archive.service;

import com.archive.common.ErrorCode;
import com.archive.config.AliyunSmsProperties;
import com.archive.exception.BusinessException;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 阿里云「短信认证」实现：SendSmsVerifyCode 发码（验证码由阿里云动态生成），
 * CheckSmsVerifyCode 校验。后端不存验证码。
 *
 * <p>校验结果取响应体 {@code body.getModel().getVerifyResult()}（String）。
 * 阿里云实网凭据缺失，本实现未做实网验证；verifyResult 字符串取值采用防御性解析
 * （"true"/"pass" 视为通过），接入真实 AK/SK 后以实际返回为准。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aliyun.sms-auth", name = "enabled", havingValue = "true")
public class AliyunSmsCodeService implements SmsCodeService {

    private final AliyunSmsProperties props;
    private Client client;

    @PostConstruct
    void initClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(props.getAccessKeyId())
                .setAccessKeySecret(props.getAccessKeySecret());
        config.endpoint = props.getEndpoint();
        this.client = new Client(config);
    }

    @Override
    public void send(String phone, String scene) {
        try {
            SendSmsVerifyCodeRequest req = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(props.getSignName())
                    .setTemplateCode(props.getTemplateCode())
                    .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                    .setCodeType(1L); // 纯数字
            SendSmsVerifyCodeResponse resp = client.sendSmsVerifyCode(req);
            log.info("[SMS-ALIYUN] 发码响应 phone={} code={} message={}",
                    phone, resp.getBody().getCode(), resp.getBody().getMessage());
        } catch (Exception e) {
            log.error("[SMS-ALIYUN] 发码失败 phone={}", phone, e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "短信发送失败，请稍后再试");
        }
    }

    @Override
    public boolean verify(String phone, String code) {
        try {
            CheckSmsVerifyCodeRequest req = new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setVerifyCode(code);
            CheckSmsVerifyCodeResponse resp = client.checkSmsVerifyCode(req);
            CheckSmsVerifyCodeResponseBody body = resp.getBody();
            if (body == null) {
                return false;
            }
            // API 调用本身失败则视为校验未通过
            if (Boolean.FALSE.equals(body.getSuccess())
                    || (body.getCode() != null && !"OK".equals(body.getCode()))) {
                log.warn("[SMS-ALIYUN] 校验接口返回非 OK phone={} code={} message={}",
                        phone, body.getCode(), body.getMessage());
                return false;
            }
            CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel model = body.getModel();
            if (model == null || model.getVerifyResult() == null) {
                return false;
            }
            String vr = model.getVerifyResult();
            return "true".equalsIgnoreCase(vr) || "pass".equalsIgnoreCase(vr);
        } catch (Exception e) {
            log.error("[SMS-ALIYUN] 校验失败 phone={}", phone, e);
            return false;
        }
    }
}
