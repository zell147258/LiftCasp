test -f ~/.sbtconfig && . ~/.sbtconfig
exec java -Xmx1024M ${SBT_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar /usr/local/Cellar/sbt/0.13.0/libexec/sbt-launch.jar "$@"
