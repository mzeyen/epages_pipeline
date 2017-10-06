FROM jenkins:1.642.4

USER root
RUN apt-get update && apt-get -y install --no-install-recommends \
      cvs \
      git \
      sudo && \
    apt-get clean

RUN usermod -a -G sudo jenkins && \
    echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
USER jenkins

# Copy list of our plugins and install them.
COPY plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt

# Copy ssh key required for connecting to slaves.
COPY files/home/jenkins/.ssh /home/jenkins/.ssh/
RUN sudo chown -R jenkins:jenkins /home/jenkins && \
    chmod 755 /home/jenkins/.ssh && \
    chmod 600 /home/jenkins/.ssh/id_rsa && \
    chmod 644 /home/jenkins/.ssh/id_rsa.pub

