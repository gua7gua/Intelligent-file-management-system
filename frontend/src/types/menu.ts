/** 侧边栏菜单项 */
export interface MenuItem {
  /** 路由路径，作为 el-menu 的 index */
  path: string
  /** 菜单标题 */
  title: string
  /** Element Plus 图标组件名 */
  icon?: string
  /** 可见角色列表 */
  roles: string[]
  /** 子菜单 */
  children?: MenuItem[]
}

/** 门户定义 */
export interface PortalDef {
  key: string
  label: string
  path: string
}
