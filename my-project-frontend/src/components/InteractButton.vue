<script setup lang="ts">

const props = defineProps({
  name: String,
  checkName: String,
  color: String,
  check: Boolean,
  disabled: Number
})

function onClick() {
  console.log(props.disabled)
  if (props.disabled === 0) {
    emit('check')
  }
}

const emit = defineEmits(['check'])
</script>

<template>
  <div class="interact-button" :class="{'locked': disabled}">
    <span class="icon" :style="{'color': check ? color : 'unset'}" @click="onClick">
      <slot/>
    </span>
    <span class="name" :style="{'color': color}">
      {{ check ? checkName : name }}
    </span>
  </div>
</template>

<style scoped>
.interact-button {
  display: inline-block;
  height: 20px;
  transition: 0.3s;

  .name {
    font-size: 14px;
    margin-left: 5px;

    &:hover {
      opacity: 0.8;
      cursor: pointer;
    }
  }

  .icon {
    vertical-align: middle;
    transition: .3s;

    &:hover {
      cursor: pointer;
      font-size: 18px;
    }
  }

  &.locked {
    opacity: 0.5;

    .icon:hover {
      font-size: unset;
      cursor: not-allowed;
    }

    .name:hover {
      cursor: not-allowed;
    }
  }

}
</style>