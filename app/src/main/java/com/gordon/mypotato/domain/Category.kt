package com.gordon.mypotato.domain

/**
 * 分类实体
 *
 * 表示任务的分类，如学习、工作、生活、健康、购物等，支持颜色和图标自定义。
 * 注意：id=0 为保留值，表示"未分类"，不用于实际分类。
 *
 * @property id 分类唯一标识
 * @property name 分类名称（必填）
 * @property colorHex 分类颜色十六进制值（如：#FF6B6B）
 * @property iconName 分类图标资源名称（可选，如：ic_category_study）
 */
data class Category(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconName: String?
)