import { ref, onMounted, onBeforeUnmount, nextTick, watch } from "vue"

export function useSearchExpand(formRef) {
  const collapsedRows = ref(1)
  const expand = ref(false)
  const showExpand = ref(false)

  let resizeObserver = null
  let mutationObserver = null

  const getFormElement = () => {
    const form = formRef?.value
    if (!form) return null
    return form.$el || form
  }

  const isElementVisible = (element) => {
    if (!element) return false
    const style = window.getComputedStyle(element)
    return style.display !== "none" && style.visibility !== "hidden" && element.offsetParent !== null
  }

  const calcShowExpand = () => {
    const formElement = getFormElement()
    if (!formElement) {
      showExpand.value = false
      expand.value = false
      return
    }

    const allItems = Array.from(formElement.querySelectorAll(".ant-form-item"))
    if (!allItems.length) {
      showExpand.value = false
      expand.value = false
      return
    }

    const visibleItems = allItems.filter(isElementVisible)
    if (!visibleItems.length) {
      showExpand.value = false
      expand.value = false
      return
    }

    const firstTop = Math.min(...visibleItems.map((item) => item.getBoundingClientRect().top))
    const firstRowCount = visibleItems.filter(
      (item) => Math.abs(item.getBoundingClientRect().top - firstTop) <= 2
    ).length

    const shouldShow = allItems.length > firstRowCount * collapsedRows.value
    showExpand.value = shouldShow

    if (!shouldShow) {
      expand.value = false
    }
  }

  const resetObservers = () => {
    if (resizeObserver) {
      resizeObserver.disconnect()
      resizeObserver = null
    }
    if (mutationObserver) {
      mutationObserver.disconnect()
      mutationObserver = null
    }

    const formElement = getFormElement()
    if (!formElement) return

    if (typeof ResizeObserver !== "undefined") {
      resizeObserver = new ResizeObserver(() => calcShowExpand())
      resizeObserver.observe(formElement)
    }

    if (typeof MutationObserver !== "undefined") {
      mutationObserver = new MutationObserver(() => calcShowExpand())
      mutationObserver.observe(formElement, {
        childList: true,
        subtree: true,
        attributes: true,
      })
    }
  }

  const handleWindowResize = () => calcShowExpand()

  onMounted(() => {
    nextTick(() => {
      calcShowExpand()
      resetObservers()
    })
    window.addEventListener("resize", handleWindowResize)
  })

  watch(
    () => formRef?.value,
    () => {
      nextTick(() => {
        calcShowExpand()
        resetObservers()
      })
    },
    { flush: "post" }
  )

  onBeforeUnmount(() => {
    window.removeEventListener("resize", handleWindowResize)
    if (resizeObserver) {
      resizeObserver.disconnect()
      resizeObserver = null
    }
    if (mutationObserver) {
      mutationObserver.disconnect()
      mutationObserver = null
    }
  })

  return {
    expand,
    showExpand,
    collapsedRows,
    calcShowExpand,
  }
}
