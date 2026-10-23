import {get} from "@/net/index.js";

//返回邮件列表
export const apiEmailList = (page,size,success) => {
  get(`api/admin/email/list?page=${page}&size=${size}`,success)
}

//重发邮件
export const apiEmailResend = (id, success,failure) => {
  get(`api/admin/email/resend?emailId=${id}`,success,failure)
}