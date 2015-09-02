FROM luci/java7:0.2

# Install a basic SSH server
RUN apt-get update && apt-get install -y openssh-server
RUN sed -i 's|session    required     pam_loginuid.so|session    optional     pam_loginuid.so|g' /etc/pam.d/sshd
RUN mkdir -p /var/run/sshd

RUN mkdir ~/.ssh && chmod 700 ~/.ssh
RUN ln -s /luci/data/jenkinsSlave/authorized_keys ~/.ssh/authorized_keys

# Set new password for luci, for slave usage
RUN echo "luci:praqma" | chpasswd

# Standard SSH port
EXPOSE 22

CMD ["/usr/sbin/sshd", "-D"]
