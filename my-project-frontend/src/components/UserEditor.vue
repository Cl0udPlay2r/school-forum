<script setup>

import {EditPen} from "@element-plus/icons-vue";
import {reactive} from "vue";
import {apiUserAccountDetailAndPrivacy, apiUserSaveAccountChange} from '@/net/api/user.js'
import {ElMessage} from "element-plus";

const props = defineProps({
  userTable: Object
})

const editPen = reactive({
  id: 0,
  display: false,
  loading: false,
  temp: {}
})
defineExpose({openEditPen})

function openEditPen(user) {
  editPen.id = user.id
  editPen.display = true
  editPen.loading = true
  apiUserAccountDetailAndPrivacy(user.id, data => {
    editPen.temp = {...data,...user}
    editPen.loading = false
  })
}

function saveUserChange() {
  editPen.display = false
  apiUserSaveAccountChange(editPen.temp,() => {
    const user = props.userTable.data.find(user => user.id === editPen.id)
    Object.assign(user,editPen.temp)
    ElMessage.success("保存成功")
  })
}
</script>

<template>
  <div>
    <el-drawer v-model="editPen.display"
               :close-on-click-modal="false">
      <template #header>
        <div style="margin-top: 10px">
          <div style="font-weight: bold">
            <el-icon><EditPen/></el-icon>
            编辑用户信息
          </div>
          <div style="font-size: 13px;color: grey">
            编辑完成后请点击下方的保存按钮
          </div>
        </div>
      </template>
      <div  v-loading="editPen.loading" style="height: 100%" element-loading-text="数据正在加载，请稍后...">
        <el-form label-position="top" v-if="!editPen.loading">
          <el-form-item label="用户名">
            <el-input v-model="editPen.temp.username" />
          </el-form-item>
          <el-form-item label="邮件">
            <el-input v-model="editPen.temp.email" />
          </el-form-item>
          <div style="display: flex;gap: 15px;">
            <div >
              <span style="margin-right: 10px">禁言</span>
              <el-switch v-model="editPen.temp.mute" />
            </div>
            <el-divider style="height: 30px" direction="vertical"/>
            <div>
              <span style="margin-right: 10px">封禁</span>
              <el-switch v-model="editPen.temp.banned" />
            </div>
          </div>

          <div style="font-size: 13px;color: grey;margin-top: 20px">
            注册时间:{{new Date(editPen.temp.registerTime).toLocaleString()}}
          </div>
          <el-divider/>
          <el-form-item label="性别">
            <el-radio-group v-model="editPen.temp.details.gender">
              <el-radio :label="0">男</el-radio>
              <el-radio :label="1">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号" >
            <el-input v-model="editPen.temp.details.phone" maxlength="11"/>
          </el-form-item>
          <el-form-item label="QQ号" >
            <el-input v-model="editPen.temp.details.qq" maxlength="13"/>
          </el-form-item>
          <el-form-item label="微信号">
            <el-input v-model="editPen.temp.details.wx" maxlength="20"/>
          </el-form-item>
          <el-form-item label="个人简介" >
            <el-input type="textarea" :rows="6" v-model="editPen.temp.details.desc" maxlength="200"/>
          </el-form-item>

          <el-divider/>
          <div class="checkbox-list">
            <el-checkbox v-model="editPen.temp.privacy.phone"
                         >公开展示我的手机号</el-checkbox>
            <el-checkbox v-model="editPen.temp.privacy.email"
                          >公开展示我的电子邮件</el-checkbox>
            <el-checkbox v-model="editPen.temp.privacy.qq"
                         >公开展示我的QQ号</el-checkbox>
            <el-checkbox v-model="editPen.temp.privacy.wx"
                          >公开展示我的微信号</el-checkbox>
            <el-checkbox v-model="editPen.temp.privacy.gender">公开展示我的性别</el-checkbox>
          </div>
        </el-form>

      </div>
      <template #footer>
        <div>
          <el-button type="primary" @click="saveUserChange">保存</el-button>
          <el-button type="info" @click="editPen.display=false" >取消</el-button>
        </div>
      </template>
    </el-drawer>
  </div>

</template>

<style scoped>
.checkbox-list{
  margin: 10px 0 0 10px;
  display: flex;
  flex-direction: column;
}
</style>