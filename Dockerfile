FROM openjdk:11 AS builder
COPY . /usr/src/openas2
WORKDIR /usr/src/openas2
# To test Locally builder environment:
# docker run --rm -it -v $(pwd):/usr/src/openas2 openjdk:11 bash
RUN rm -f Server/dist/*
RUN rm -f Remote/dist/*
RUN rm -f Bundle/dist/*
RUN sed -i 's/\r//' ./mvnw
ENV AWS_ACCESS_KEY_ID=<YOUR_AWS_ACCESS_KEY_ID>
ENV AWS_SECRET_ACCESS_KEY=<YOUR_AWS_SECRET_ACCESS_KEY>
ENV AWS_REGION=<YOUR_AWS_REGION>
RUN ./mvnw clean package
RUN mkdir ./Runtime && unzip Server/dist/OpenAS2Server-*.zip -d Runtime
RUN ./mvnw clean
COPY start-container.sh /usr/src/openas2/Runtime/bin/
RUN cd /usr/src/openas2/Runtime/bin && \
    chmod 755 *.sh && \    
    sed -i 's/\r//' *.sh && \ 
    cd /usr/src/openas2/Runtime && \
    mv config config_template


FROM openjdk:11-jre-slim 
ENV OPENAS2_BASE=/opt/openas2
ENV OPENAS2_HOME=/opt/openas2
ENV OPENAS2_TMPDIR=/opt/openas2/temp
ENV AWS_ACCESS_KEY_ID=<YOUR_AWS_ACCESS_KEY_ID>
ENV AWS_SECRET_ACCESS_KEY=<YOUR_AWS_SECRET_ACCESS_KEY>
ENV AWS_REGION=<YOUR_AWS_REGION>
COPY --from=builder /usr/src/openas2/Runtime/bin ${OPENAS2_BASE}/bin
COPY --from=builder /usr/src/openas2/Runtime/lib ${OPENAS2_BASE}/lib
COPY --from=builder /usr/src/openas2/Runtime/resources ${OPENAS2_BASE}/resources
COPY --from=builder /usr/src/openas2/Runtime/config_template ${OPENAS2_HOME}/config_template
RUN mkdir ${OPENAS2_BASE}/config
WORKDIR $OPENAS2_HOME
ENTRYPOINT ${OPENAS2_BASE}/bin/start-container.sh