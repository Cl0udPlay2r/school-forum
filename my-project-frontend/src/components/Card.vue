<script setup>
defineProps({
  icon: Object,
  title: String,
  desc: String
})
</script>

<template>
  <div class="card">
    <div class="card-header" v-if="title">
      <el-icon style="margin-right: 3px;translate: 0 2px">
        <component :is="icon"/>
      </el-icon>
      {{title}}
      <div>{{desc}}</div>
    </div>
    <slot/>
  </div>
</template>

<style scoped>
.card {
  border-radius: 5px;
  border-bottom: 1px solid var(--el-border-color);
  background-color: var(--el-bg-color);
  box-sizing: border-box;
  min-height: 20px;
  padding: 10px;
}

.card-header {
  border-bottom: 1px solid var(--el-border-color);
  margin-bottom: 10px;
  padding-bottom: 5px;

  &>:first-child{
    font-size: 18px;
    font-weight: bold;
  }

  &>:last-child{
    font-size: 13px;
    color: gray;
  }
}
</style>