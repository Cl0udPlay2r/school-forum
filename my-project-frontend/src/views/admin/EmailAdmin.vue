<script setup lang="js">

import {RefreshRight, User} from "@element-plus/icons-vue";
import {reactive, watchEffect} from "vue";
import {apiEmailList, apiEmailResend} from '@/net/api/email.js'
import {ElMessage, ElMessageBox} from "element-plus";

const emailList = reactive({
  page: 1,
  size: 10,
  total: 0,
  list: []
})

const resendEmail = (row) => {
  const {id, email} = row
  ElMessageBox.confirm(`确定要重发邮件：${email}吗？`, "重发邮件", {
    callback: value => {
      if(value === 'confirm'){
        apiEmailResend(id,() =>{
          ElMessage.success("邮件重发成功")
          row.status=0
        },() => {
          ElMessage.warning("邮件重发失败")
          row.status=2
        })
      }
    }
  })
}

watchEffect(() => {
  apiEmailList(emailList.page, emailList.size, (data) => {
    emailList.list = data.list;
    emailList.total = data.total;
  })
})
</script>

<template>
  <div class="email-admin">
    <div class="email-admin-title">
      <el-icon>
        <User/>
      </el-icon>
      邮件管理
    </div>
    <div class="email-admin-desc">
      在这里管理所有发送的邮件信息
    </div>
    <el-table :data="emailList.list" height="500">
      <el-table-column prop="id" label="ID" width="50" align="center"/>
      <el-table-column prop="email" label="收件人" width="200"/>
      <el-table-column label="状态" align="center" width="80">
        <template #default="{row}">
          <el-tag v-if="row.status === 0" type="info" size="small">发送中</el-tag>
          <el-tag v-if="row.status === 1" type="success" size="small">已发送</el-tag>
          <el-tag v-if="row.status === 2" type="warning" size="small">发送失败</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="邮件主题" width="200"/>
      <el-table-column prop="content" label="邮件内容" align="center"
                       show-overflow-tooltip/>
      <el-table-column label="发送时间" width="200" show-overflow-tooltip>
        <template #default="{row}">
          {{ new Date(row.time).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column fixed="right" width="150">
        <template #default="{row}">
          <el-button :icon="RefreshRight" type="success"
                     @click="resendEmail(row)"
                     :disabled="row.status !== 2">重新发送
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination :total="emailList.total"
                     v-model:current-page="emailList.page"
                     v-model:page-size="emailList.size"
                     layout="total, sizes ,prev, pager, next, jumper"/>
    </div>
  </div>
</template>

<style scoped>
.email-admin {
  .email-admin-title {
    font-weight: bold;
    margin: 20px 0 15px 20px;
  }

  .email-admin-desc {
    margin: 20px 0 10px 25px;
    color: #bababa;
    font-size: 13px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: right;
  }
}
</style>