import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { workspaceAPI } from '@/api/workspace'
import { useUserStore } from '@/stores/user'

const WORKSPACE_SELECTION_KEY = 'workspaceSelection'
const PERSONAL_SPACE_NAME = '个人空间'

const workspaceKey = (id) => (id === null || id === undefined ? '' : String(id))

const isSameWorkspace = (left, right) => workspaceKey(left) === workspaceKey(right)

/**
 * 使用纯 JavaScript 解码 JWT 载荷，避免小程序环境缺少浏览器 atob API。
 *
 * @param {string} token 登录令牌
 * @returns {object|null} JWT 载荷，令牌不合法时返回 null
 */
const decodeJwtPayload = (token) => {
  try {
    const segment = String(token || '').split('.')[1]
    if (!segment) return null

    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
    const base64 = segment.replace(/-/g, '+').replace(/_/g, '/')
    let bits = 0
    let bitCount = 0
    let encoded = ''

    for (const char of base64.replace(/=+$/g, '')) {
      const value = alphabet.indexOf(char)
      if (value < 0) continue
      bits = (bits << 6) | value
      bitCount += 6
      if (bitCount >= 8) {
        bitCount -= 8
        encoded += `%${((bits >> bitCount) & 0xff).toString(16).padStart(2, '0')}`
      }
    }

    return JSON.parse(decodeURIComponent(encoded))
  } catch (_) {
    return null
  }
}

const readInitialSelection = () => {
  const stored = uni.getStorageSync(WORKSPACE_SELECTION_KEY)
  if (stored && stored.id !== null && stored.id !== undefined) {
    return { id: stored.id, name: stored.name || '协作工作区' }
  }

  const payload = decodeJwtPayload(uni.getStorageSync('token'))
  if (payload && payload.currentWsId !== null && payload.currentWsId !== undefined) {
    return { id: payload.currentWsId, name: '协作工作区' }
  }
  return { id: null, name: PERSONAL_SPACE_NAME }
}

export const useWorkspaceStore = defineStore('workspace', () => {
  const initialSelection = readInitialSelection()
  const workspaces = ref([])
  const currentId = ref(initialSelection.id)
  const currentName = ref(initialSelection.name)
  const loading = ref(false)
  const switchingId = ref(null)
  const loadError = ref('')

  const currentWorkspace = computed(() =>
    workspaces.value.find((workspace) => isSameWorkspace(workspace.id, currentId.value)) || null,
  )
  const hasMultiple = computed(() => workspaces.value.length > 0)
  const switching = computed(() => switchingId.value !== null)

  const saveSelection = (id, name) => {
    currentId.value = id
    currentName.value = name || PERSONAL_SPACE_NAME
    if (id === null || id === undefined) {
      uni.removeStorageSync(WORKSPACE_SELECTION_KEY)
      return
    }
    uni.setStorageSync(WORKSPACE_SELECTION_KEY, { id, name: currentName.value })
  }

  const syncSelectionWithList = () => {
    if (currentId.value === null || currentId.value === undefined) {
      saveSelection(null, PERSONAL_SPACE_NAME)
      return
    }

    const selected = workspaces.value.find((workspace) =>
      isSameWorkspace(workspace.id, currentId.value),
    )
    if (selected) {
      saveSelection(selected.id, selected.name)
      return
    }

    // 列表是后端当前可访问范围的权威结果，失效或被移除的工作区不能继续留在选中态。
    saveSelection(null, PERSONAL_SPACE_NAME)
  }

  /**
   * 获取当前用户可访问的工作区，并校正本地选中状态。
   *
   * @returns {Promise<Array>} 工作区列表
   */
  const fetchWorkspaces = async () => {
    if (loading.value) return workspaces.value
    try {
      loading.value = true
      loadError.value = ''
      const data = await workspaceAPI.list()
      workspaces.value = Array.isArray(data) ? data : []
      syncSelectionWithList()
      return workspaces.value
    } catch (error) {
      loadError.value = error.message || '工作区加载失败'
      return workspaces.value
    } finally {
      loading.value = false
    }
  }

  /**
   * 切换到协作工作区，并使用后端签发的新令牌更新后续请求上下文。
   *
   * @param {number|string} id 工作区 ID
   * @returns {Promise<object|null>} 后端切换结果
   */
  const switchWorkspace = async (id) => {
    if (switching.value || isSameWorkspace(id, currentId.value)) return null
    try {
      switchingId.value = workspaceKey(id)
      const result = await workspaceAPI.switchWorkspace(id)
      if (!result || !result.token) throw new Error('服务端未返回新的工作区凭证')

      useUserStore().setToken(result.token)
      const selected = workspaces.value.find((workspace) => isSameWorkspace(workspace.id, id))
      saveSelection(selected ? selected.id : id, selected?.name || '协作工作区')
      return result
    } finally {
      switchingId.value = null
    }
  }

  /**
   * 切回个人空间，并清除协作工作区的本地选择。
   *
   * @returns {Promise<object|null>} 后端切换结果
   */
  const switchToPersonal = async () => {
    if (switching.value || currentId.value === null || currentId.value === undefined) return null
    try {
      switchingId.value = 'personal'
      const result = await workspaceAPI.switchToPersonal()
      if (!result || !result.token) throw new Error('服务端未返回新的个人空间凭证')

      useUserStore().setToken(result.token)
      saveSelection(null, PERSONAL_SPACE_NAME)
      return result
    } finally {
      switchingId.value = null
    }
  }

  /**
   * 判断给定工作区是否为当前选中项。
   *
   * @param {number|string|null} id 工作区 ID，null 表示个人空间
   * @returns {boolean} 是否为当前工作区
   */
  const isCurrent = (id) => {
    if (id === null || id === undefined) {
      return currentId.value === null || currentId.value === undefined
    }
    return isSameWorkspace(id, currentId.value)
  }

  return {
    workspaces,
    currentId,
    currentName,
    loading,
    switchingId,
    switching,
    loadError,
    currentWorkspace,
    hasMultiple,
    fetchWorkspaces,
    switchWorkspace,
    switchToPersonal,
    isCurrent,
  }
})
