/**
 * 配置層 (Configuration Layer - The Dirtiest Layer)
 * <p>
 * 全域配置集中存放套件，包含 Spring Configuration ({@code @Configuration})、Bean 註冊、
 * Web 設定、Security 等技術耦合度極高的配置檔。
 * 專門負責將所有層與基礎設施組裝起來，嚴禁將配置類散落於 Domain、Application 或其他分層模組內。
 */
package com.example.demo.config;
