<template>
  <div>
    <h2>管理概览</h2>
    <p style="color: #909399; margin-top: 8px;">待实现：馆藏总量、本月新增、待办提醒等统计卡片</p>
    <el-row :gutter="20" style="margin-top: 24px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">馆藏总量</p>
          <p style="font-size: 28px; font-weight: 700; color: #303133;">—</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">本月新增</p>
          <p style="font-size: 28px; font-weight: 700; color: #409eff;">—</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">待入库</p>
          <p style="font-size: 28px; font-weight: 700; color: #e6a23c;">—</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">待审批借阅</p>
          <p style="font-size: 28px; font-weight: 700; color: #f56c6c;">—</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
