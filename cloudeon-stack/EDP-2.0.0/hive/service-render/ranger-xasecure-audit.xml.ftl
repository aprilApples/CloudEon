<!-- MySQL 的连接地址及端口号，须与 Ranger Admin 中 install.properties 配置相同 -->
<property>
  <name>xasecure.audit.jpa.javax.persistence.jdbc.url</name>
  <value>${conf['ranger.xasecure.audit.jdbc.url']}</value>
</property>

<!-- 指定所使用用户 -->
<property>
  <name>xasecure.audit.jpa.javax.persistence.jdbc.user</name>
  <value>${conf['ranger.xasecure.audit.jdbc.user']}</value>
</property>

<!-- 指定用户密码 -->
<property>
  <name>xasecure.audit.jpa.javax.persistence.jdbc.password</name>
  <value>${conf['ranger.xasecure.audit.jdbc.password']}</value>
</property>