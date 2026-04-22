import { ref, onMounted, onBeforeUnmount, nextTick } from "vue"

export function useTableScroll(options = {}) {
  const {
    minY = 240,
    extraOffset = 8,
    fixedSelectors = [".operation-wrapper"],
  } = options

  const containerRef = ref(null)
  const tableScrollY = ref(minY)

  let resizeObserver = null
  let mutationObserver = null

  const getElementOuterHeight = (element) => {
    if (!element) return 0
    const style = window.getComputedStyle(element)
    const marginTop = parseFloat(style.marginTop || "0")
    const marginBottom = parseFloat(style.marginBottom || "0")
    return element.offsetHeight + marginTop + marginBottom
  }

  const calcTableScrollY = () => {
    const container = containerRef.value
    if (!container) return

    const pagination = container.querySelector(".ant-pagination")
    const paginationHeight = getElementOuterHeight(pagination)

    const fixedElementsHeight = fixedSelectors.reduce((sum, selector) => {
      const element = container.querySelector(selector)
      return sum + getElementOuterHeight(element)
    }, 0)

    const containerStyle = window.getComputedStyle(container)
    const paddingTop = parseFloat(containerStyle.paddingTop || "0")
    const paddingBottom = parseFloat(containerStyle.paddingBottom || "0")

    const paginationReserved = Math.max(0, paginationHeight - paddingBottom)

    const availableHeight =
      container.clientHeight -
      fixedElementsHeight -
      paginationReserved -
      paddingTop -
      paddingBottom -
      extraOffset

    tableScrollY.value = Math.max(minY, Math.floor(availableHeight))
  }

  const handleResize = () => {
    calcTableScrollY()
  }

  onMounted(() => {
    nextTick(calcTableScrollY)
    window.addEventListener("resize", handleResize)

    if (typeof ResizeObserver !== "undefined") {
      resizeObserver = new ResizeObserver(() => calcTableScrollY())
      if (containerRef.value) {
        resizeObserver.observe(containerRef.value)
      }
    }

    if (typeof MutationObserver !== "undefined" && containerRef.value) {
      mutationObserver = new MutationObserver(() => calcTableScrollY())
      mutationObserver.observe(containerRef.value, {
        childList: true,
        subtree: true,
        attributes: true,
      })
    }
  })

  onBeforeUnmount(() => {
    window.removeEventListener("resize", handleResize)
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
    containerRef,
    tableScrollY,
    calcTableScrollY,
  }
}
