<script setup>

defineProps({
  data: Object
})

</script>

<template>
  <div style="height: 200px" v-loading="!data.success"
       element-loading-text="天气信息加载中..." >
    <div style="display: flex;justify-content: space-between;margin: 10px 20px" v-if="data.success">
      <div style="font-size: 40px;color: gray">
        <i :class="`qi-${data.now.icon}`"></i>
      </div>
      <div style="text-align: center">
        <div style="font-size: 25px;font-weight: bold">{{data.now.temp}}℃</div>
        <div style="font-size: 15px">{{data.now.text}}</div>
      </div>
      <div>
        <div style="text-align: center;translate: 0 5px">{{data.location.adm1}}<br/>{{data.location.adm2}}</div>
      </div>
    </div>
    <el-divider style="margin: 10px"/>
    <div style="display: grid;grid-template-columns: repeat(5,1fr);text-align: center">
      <div v-for="item in data.hourly">
        <div>{{new Date(item.fxTime).getHours()}}点</div>
        <div style="font-size: 20px;margin: 10px 0 10px 0">
          <i :class="`qi-${item.icon}-fill`"></i>
        </div>
        <div style="font-size: 14px">{{item.temp}}℃</div>
      </div>
    </div>
  </div>

</template>

<style scoped>

</style>