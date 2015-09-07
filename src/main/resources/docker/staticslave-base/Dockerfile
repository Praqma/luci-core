FROM luci/base:0.2

RUN apt-get update && apt-get install -y git subversion mercurial

# Define git user and email
RUN git config --global user.email "noreply@example.com" && git config --global user.name "LUCI"
