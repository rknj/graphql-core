FROM circleci/maven@1.0.3

ADD . /tmp
WORKDIR /tmp

# Jahia-cli is used to warmup the environment at startup
RUN yarn add jahia-cli@0.0.47

CMD ["/bin/bash", "-c", "/tmp/env.run.sh"]
