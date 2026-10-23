<script setup>

import Card from "@/components/Card.vue";
import {Guide, Lock, Odometer, Refresh} from "@element-plus/icons-vue";
import {onMounted, reactive, ref} from "vue";
import {ElMessage} from "element-plus";
import {apiUserChangePassword, apiUserPrivacy, apiUserPrivacySave} from "@/net/api/user.js";

const form = reactive({
  password: '',
  new_password: '',
  new_password_repeat: ''
})

const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.new_password) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const rule = ({
  password: [
    { required: true, message: '请输入原密码', trigger: ['blur' ,'change'] },
    { min: 6, max: 20, message: '密码的长度必须在6-20个字符之间', trigger: ['blur', 'change'] }
  ],
  new_password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码的长度必须在6-20个字符之间', trigger: ['blur', 'change'] }
  ],
  new_password_repeat: [
    { validator: validatePassword, trigger: ['blur', 'change'] },
  ],
})

const formRef = ref()
const valid = ref(false)

const onValidate = (prop,isValida) => valid.value = isValida

function changerPassword(){
  formRef.value.validate(isValid =>{
    if(isValid){
      apiUserChangePassword(form,() => {
        ElMessage.success("密码重置成功")
        formRef.value.resetFields()
      })
    }
  })
}

const privacy = reactive({
  phone: false,
  email: false,
  qq: false,
  wx: false,
  gender: false
})

const saving = ref(true)


function savePrivacy(type,status){
  apiUserPrivacySave(type,status,saving)
}

onMounted(() => {
  apiUserPrivacy(data => {
    Object.assign(privacy,data)
    saving.value = false
  })
})

</script>

<template>
  <div style="max-width: 600px;margin: auto">
    <div style="margin-top: 20px">
      <card :icon="Guide" title="隐私设置" desc="在这里你可以设置哪些内容可以被其他人看到，请注意自己的隐私" v-loading="saving">
        <div class="checkbox-list">
          <el-checkbox v-model="privacy.phone"
                       @change="savePrivacy('phone',privacy.phone)">公开展示我的手机号</el-checkbox>
          <el-checkbox v-model="privacy.email"
                       @change="savePrivacy('email',privacy.email)">公开展示我的电子邮件</el-checkbox>
          <el-checkbox v-model="privacy.qq"
                       @change="savePrivacy('qq',privacy.qq)">公开展示我的QQ号</el-checkbox>
          <el-checkbox v-model="privacy.wx"
                       @change="savePrivacy('wx',privacy.wx)">公开展示我的微信号</el-checkbox>
          <el-checkbox v-model="privacy.gender"
                       @change="savePrivacy('gender',privacy.gender)">公开展示我的性别</el-checkbox>
        </div>
      </card>
      <card :icon="Odometer" title="修改密码" desc="在这里修改你的密码，请务必牢记您的新密码">
        <el-form :model="form" :rules="rule" label-width="100" ref="formRef" @validate="onValidate" style="margin: 20px;">
          <el-form-item  prop="password" label="当前密码">
            <el-input v-model="form.password" :prefix-icon="Lock" type="password" placeholder="当前密码"/>
          </el-form-item>
          <el-form-item  prop="new_password" label="新密码">
            <el-input  v-model="form.new_password" :prefix-icon="Lock" type="password" placeholder="新密码"/>
          </el-form-item>
          <el-form-item  prop="new_password_repeat" label="重复新密码">
            <el-input  v-model="form.new_password_repeat" :prefix-icon="Lock" type="password" placeholder="重新输入新密码"/>
          </el-form-item>
          <div style="text-align: center;margin-top: 30px">
            <el-button :icon="Refresh" @click="changerPassword" type="success" plain>立即重置密码</el-button>
          </div>
        </el-form>
      </card>
    </div>
  </div>
</template>

<style scoped>
.checkbox-list{
  margin: 10px 0 0 10px;
  display: flex;
  flex-direction: column;
}
</style>