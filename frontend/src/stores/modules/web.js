import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useWebStore = defineStore('web', () => {
    const menuCollapse = ref(false)
    const menuSelect = ref([])
    const category = ref({
        currentSelect: ''
    })
    function selectCategory(item) {
        category.value.currentSelect = item
    }
    function setMenuCollapse(val) {
        menuCollapse.value = val
    }
    function setMenuSelect(val) {
        menuSelect.value = val
    }
    return {
        menuCollapse,
        menuSelect,
        category,
        selectCategory,
        setMenuCollapse,
        setMenuSelect
    }
}, {
    persist: true,
},)

export default useWebStore