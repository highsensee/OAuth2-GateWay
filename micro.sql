/*
 Navicat Premium Data Transfer

 Source Server         : centos-master
 Source Server Type    : MySQL
 Source Server Version : 80032
 Source Host           : 192.168.200.128:3306
 Source Schema         : micro

 Target Server Type    : MySQL
 Target Server Version : 80032
 File Encoding         : 65001

 Date: 10/05/2023 19:14:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for client
-- ----------------------------
DROP TABLE IF EXISTS `client`;
CREATE TABLE `client`  (
  `client_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `resource_ids` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `secret_require` int(0) NULL DEFAULT NULL,
  `client_secret` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scope_require` int(0) NULL DEFAULT NULL,
  `scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `authorized_grant_types` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `web_server_redirect_uris` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `authorities` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `access_token_validity` int(0) NULL DEFAULT NULL,
  `refresh_token_validity` int(0) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of client
-- ----------------------------
INSERT INTO `client` VALUES ('client-app', 'oauth2-resource', 0, '$2a$10$yVcK4CcnMiLeTExYz89UIOU.pn89o9tkisDSjWQ7Jn9.V0Ww8ykJC', 0, 'all', 'password,refresh_token', NULL, 'ADMIN,USER', 3600, 86400);
INSERT INTO `client` VALUES ('client-app-2', 'oauth2-resource2', 0, '$2a$10$yVcK4CcnMiLeTExYz89UIOU.pn89o9tkisDSjWQ7Jn9.V0Ww8ykJC', 0, 'all', 'authorization_code,refresh_token', 'https://www.gathub.cn,https://www.baidu.com', 'USER', 3600, 86400);

-- ----------------------------
-- Table structure for role_resource
-- ----------------------------
DROP TABLE IF EXISTS `role_resource`;
CREATE TABLE `role_resource`  (
  `id` bigint(0) NULL DEFAULT NULL,
  `resource_path` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `resource_roles` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_resource
-- ----------------------------
INSERT INTO `role_resource` VALUES (20001, '/api/hello', 'ADMIN');
INSERT INTO `role_resource` VALUES (20002, '/api/user/currentUser', 'ADMIN,USER');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `realname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int(0) NULL DEFAULT NULL,
  `roles` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10003 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (10001, 'highsense', '张三', '$2a$10$0ieoBnPNTn9wVi6V004/DO4lkaz.4bKJOtX/4rYU52Dl.FdcXFp86', '阿德民', '13383955742', '346395848@qq.com', '河南省漯河市', 1, 'ADMIN,USER');
INSERT INTO `user` VALUES (10002, 'lubenwei', '卢本伟', '$2a$10$1Ab4FjwI.A6YFF.x1vTdfudAHkDHR/7XL63d2hwccMY3O7AGZSwPy', '卢老爷', '15839547476', '278177794@qq.com', '广东省湛江市', 1, 'USER');

SET FOREIGN_KEY_CHECKS = 1;
