<script setup>

import {useStore} from "@/store/index.js";

const store = useStore();

defineProps({
    type: Number
})
</script>

<template>
    <div class="topic-type"
         :style="{
              color: store.findTypeById(type)?.color + 'EE',
              'border-color': store.findTypeById(type)?.color + '77',
              'background-color': store.findTypeById(type)?.color + '33',
         }">
        {{ store.findTypeById(type)?.name }}
    </div>
</template>

<style scoped>
.topic-type {
    display: inline-block;
    border: solid 0.5px gray;
    border-radius: 3px;
    font-size: 13px;
    padding: 0 3px;
    height: 18px;
}
</style>