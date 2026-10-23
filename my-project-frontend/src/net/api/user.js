import {useStore} from "@/store/index.js";
import {get, post} from '@/net/index.js'
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

export const apiUserInfo = (loadingRef) => {
  loadingRef.value = true
  const store = useStore()
  get('/api/user/info', (data) => {
    store.user = data
    loadingRef.value = false
  })
}

export const apiAuthRegister = (data) => {
  post('/api/auth/register', data, () => {
    ElMessage.success('注册成功，欢迎加入我们')
    router.push("/")
  })
}

export const apiAuthAskCode = (email, coldTime,type = 'register') => {
  coldTime.value = 60
  get(`/api/auth/ask-code?email=${email}&type=${type}`, () => {
    ElMessage.success(`验证码已发送到邮箱: ${email}，请注意查收`)
    const handle = setInterval(() => {
      coldTime.value--
      if(coldTime.value === 0) {
        clearInterval(handle)
      }
    }, 1000)
  }, undefined, (message) => {
    ElMessage.warning(message)
    coldTime.value = 0
  })
}

export const apiAuthResetConfirm = (data, activeRef) => {
  post('/api/auth/reset-confirm', data, () => activeRef.value++)
}

export const apiAuthResetPassword = (data) => {
  post('/api/auth/reset-password', data, () => {
    ElMessage.success('密码重置成功，请重新登录')
    router.push('/')
  })
}

export const apiUserPrivacy = (success) => {
  get('/api/user/privacy',success)
}

export const apiUserPrivacySave = (typeRef,statusRef,savingRef) => {
  savingRef.value = true
  post('/api/user/save-privacy',{
    type: typeRef,
    status: statusRef
  },() => {
    ElMessage.success("隐私设置修改成功！")
    savingRef.value = false
  })
}

export const apiUserChangePassword = (data,success) => {
  post('/api/user/change-password',data,success)
}

export const apiUserDetail = (success) => {
  get('/api/user/details',success)
}

export const apiUserDetailsSave = (form , success , failure) => {
  post('/api/user/save-details', form, success ,failure)
}

export const apiUserModifyEmail = (data,success) => {
  post('/api/user/modify-email', data,success)
}

//用户通知列表
export const apiUserLoadNotification = (success) => {
  get('/api/notification/list' ,success)
}

//删除通知
export const apiUserDeleteNotification = (id,success) => {
  get(`/api/notification/delete?id=${id}`,success)
}

//查询用户列表（管理员权限
export const apiUserAccountList = (page, size, keyword, success) => {
  get(`/api/admin/list?page=${page}&size=${size}&keyword=${keyword}`, success)
}

//查询用户详细信息和隐私信息供管理员编辑
export const apiUserAccountDetailAndPrivacy = (id, success) => {
  get(`api/admin/details?id=${id}`, success)
}

//保存管理员对用户的信息修改
export const apiUserSaveAccountChange = (data, success) => {
  post('/api/admin/save',data,success)
}

//管理员修改用户密码
export const apiUserModifyPassword = (data, success) => {
  post('/api/admin/change-password',data,success)
}
