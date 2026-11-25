package com.qykj.finmaster.common.core.domain.constansts;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
public class WxConstants {

    public static final String WX_PAY_CURRENCY_CNY = "CNY";
    public static final String WX_PAY_ORDER_NO_PRE = "WXPAY";
    public static final String WX_PAY_ORDER_REFUND_NO_PRE = "WXREFUND";
    public static final Long WX_PAY_TIME_OUT_INTERVAL = 120L;
    public static final String DEFAULT_PAYMENT_NAME = "hzzhWxApplet";
    public static final String WX_PAY_PREPAY_PRE = "prepay_id=";

    //微信支付状态
    public interface TradeState {
        public final String SUCCESS = "SUCCESS";//支付成功
        public final String REFUND = "REFUND";//转入退款
        public final String NOTPAY = "NOTPAY";//未支付
        public final String CLOSED = "CLOSED";//已关闭
    }

    public static final String WORK_WX_ACCESS_TOKEN = "";
    public static final String WORK_WX_CORP_ID = "wwd188a7230f2e14be";
    public static final String WORK_WX_MR_SECRET = "_B_ZSSFn-wItu31qVFkUrQPiQ16cllox6rj_TT4MTMw";

    /* ===== 企业微信「会话内容存档」(msgaudit) PoC 配置（工单 73 / WXMSG-01） =====
     * corpid 复用上面的 WORK_WX_CORP_ID。
     * TODO(物料未就位)：以下三项需业务侧提供后填入——
     *   1) 会话存档 secret：管理后台→安全与管理→会话内容存档→API 配置；
     *   2) RSA 私钥 PEM（公钥配到后台、私钥本地留存，用于解 encrypt_random）；
     *   3) 公钥版本号 publickey_ver（与后台所配公钥版本一致）。
     * PoC 沿用现有内联模式；生产加密为后续优化项，不在本工单范围。 */
    /** 会话存档 secret（待业务侧提供） */
    public static final String WORK_WX_MSGAUDIT_SECRET = "3R7n70n-OxEFSHDbFyZi75gEifeK8fq_GFVvJoZU2BE";
    /** 会话存档 RSA 私钥 PEM（PKCS#8，含 -----BEGIN PRIVATE KEY----- 头尾；待业务侧提供） */
    public static final String WORK_WX_MSGAUDIT_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEugIBADANBgkqhkiG9w0BAQEFAASCBKQwggSgAgEAAoIBAQD0fB653Tvbxefa\n" +
            "dZG+x3km44kLY+orxFfFyFUB4y92ao0doTd9lOTMehMk4iB/fKomF9B0aXnsL1H4\n" +
            "SQGkQXC7aXUwak2OP2QwkRZkW2W7QiMEgvWMgyeOgX3EAWPRTy34to2efueV1oIh\n" +
            "OpkWYYoF7tKoPSl56Y/78btjgj3iJzGCU6Mo8X8As87/ul4jreJyNaOw3ZIqR/sK\n" +
            "T7uuCLjwjQngUKQ9nNS/6DiLHWPp9Yofa/Eehk9bPJ0awPa2PTCH7AcpI0GTJ05b\n" +
            "IKMIfjvzs2oKB4EUsqezApjfk2WdvXOoSMCSyAiohPW9aKoSha7wrVSAVuNub5Tp\n" +
            "t4WXf923AgMBAAECggEAF3d0cY2pKbKoywC+8QtvdwqfBF9eQj4x2JOeiTRXwWt/\n" +
            "yQjI7T5l6bSL8t3blbEPGeHuScY4BDIUkZrcU/pOCkcYjwGdGRq/kKPUfOSC8sLp\n" +
            "3ox9ajPKj7ShMDeb76LEfFt1Y3G14913lx3rRMku+NvQD4LzYLbI8btlq6r8KqE9\n" +
            "aRrwZ1F5EQnuaHUf5Cs/FsAVG+fW9XqERkBrF5j3H9aY9hxAL3dkgjGWl4yfont3\n" +
            "Lowa2/nKuiFDx0+1UPBKYrSB8ozn5BEkWrmqQ2fN0YhKRc1v5pHUlELlguw5bJNC\n" +
            "9kdYzPhPFSOc475lZBSdq4gww+cjATTxHVxlp7We8QKBgQD9PvfTPVB2smu1hySv\n" +
            "NH+3FPS1kMgBYHVzTQ4dB+iOoA0xLe54GOSMk6Wv/8r0R3qUqePqcx2OOnTT78xs\n" +
            "7dosyIyu8JsramtfhPAiukUheULjrxrAaPhGFsDvxiWsOwqfJ3HoZp0yIW0V/ocJ\n" +
            "BAdYAaJbIDK+fCdJLhUtRx3TRwKBgQD3JMLa1j+HYST7E0+HcmuXBsRdVBNqCFU3\n" +
            "T//R91+aDdmmpvraVcarSQIEpFdp9AgWPUw6BmZiwaxr2R2TQk76l0+w0HTw+/JO\n" +
            "cAp9x45qAWeEvnBll51b2R5oDb07CB8UqMpY8Wrz7tH05Kf/aXMOd870kdCjqSIM\n" +
            "cBnzYgN6EQKBgGXZDihJb3PdO84sxMcl0PtblTL0QEu1dGbjpXptdMzvd37A3zgz\n" +
            "oq8arjcDI6dsm9LwJHZkx40s21FlDoz8d5MtMM8mevFSAk7PExiUWzskHo+dG6Ao\n" +
            "Vzkw63VC9mG2+QWbAoeZxTylDs8ap0or8ZwmRBpr75yGJ/KZcvDbuhX/AoGAaxFZ\n" +
            "+URoMVY8lyk+gwdm5P7hgy0xW/ZQNK5LvcLVgAAQEsq0UnJmpz3xyeEInbZC6p+T\n" +
            "rm+nW5FcCTz+OxW28Ykr1QX6I4yC5MhVQzh40iPZnQCD4P9WBLyDF8EcAb76+BBH\n" +
            "2VB9bUBBZT7KKTSdy97Lodip8r0LKyhFldMYfoECf0XTEkSyKeYOyC8zaH5I2+io\n" +
            "ZnQMkYbkSRgSw2EUOx21k+VtdtkHeQiAqFEuawTUFB9RMdXlt9hF+qJjhxl0zVK8\n" +
            "MmQEdgVsK0hPbSNXO8aBnUb/ZGDSh/pdBKWXYlSvOe6b37sNxAK0lhysj5jpH9nk\n" +
            "uLDrEiljAE+wcWCf14c=\n" +
            "-----END PRIVATE KEY-----";
    /** 会话存档公钥版本号（与后台配置一致；默认 1，待确认） */
    public static final Integer WORK_WX_MSGAUDIT_PUBLICKEY_VER = 1;

    /* ===== 企业微信回调（接收消息/事件）Token & EncodingAESKey（工单 WCB-01/02） =====
     * 在企业微信管理后台「应用 / 客户联系」的「接收消息」配置里设置，并把以下两组填上：
     *   - APP 回调：自建应用「接收消息」配置（msgaudit_notify 等事件） → /workWx/callback/app
     *   - CONTACT 回调：客户联系「客户变更回调」配置（change_external_chat 等） → /workWx/callback/contact
     * EncodingAESKey 必须为 43 位。留空时回调控制器忽略该路径、不抛异常（待业务侧提供后填入）。 */
    /** APP 回调 Token（自建应用接收消息，待业务侧提供） */
    public static final String WORK_WX_APP_CALLBACK_TOKEN = "pwosnO6YW968eszdC";
    /** APP 回调 EncodingAESKey（43 位，待业务侧提供） */
    public static final String WORK_WX_APP_CALLBACK_AESKEY = "qCGGnzMDsyArQgwH3oPiO1NL3MVDSdXuenNvueyD6PW";
    /** CONTACT 回调 Token（客户联系变更回调，待业务侧提供） */
    public static final String WORK_WX_CONTACT_CALLBACK_TOKEN = "HLQyL6cRPj4xcrJprPL59Bgr3d";
    /** CONTACT 回调 EncodingAESKey（43 位，待业务侧提供） */
    public static final String WORK_WX_CONTACT_CALLBACK_AESKEY = "RivlUXagPo9E5KKWkkflwshKOWaqKFmlgsOUx3FYh8I";

    public static String GET_WORK_WX_ACCESS_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={0}&corpsecret={1}";
    public static String GET_WORK_WX_GROUP_CHAT_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/groupchat/list?access_token={0}";
    public static String GET_WORK_WX_GROUP_CHAT_DETAIL_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/groupchat/get?access_token={0}";
    public static String GET_WORK_WX_APPCHAT_SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/appchat/send?access_token={0}";
    public static String GET_WORK_WX_MEDIA_UPLOAD_URL = "https://qyapi.weixin.qq.com/cgi-bin/media/upload?access_token={0}&type={1}";
    public static String GET_WORK_WX_USER_LIST_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/list_id?access_token={0}";


}