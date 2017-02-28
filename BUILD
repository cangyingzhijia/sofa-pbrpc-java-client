java_jar(
    name = 'java_sofa_pbrpc',
    srcs = [
        'src/com/cangyingzhijia/rpc/sofa',
    ],
    deps = [
        ':protobuf_java',
        '//thirdparty/sofa/pbrpc:proto',
        '//thirdparty/sofa/java/lib:mina-core-2.0.9',
        '//thirdparty/sofa/java/lib:slf4j-simple-1.7.13',
        '//thirdparty/sofa/java/lib:slf4j-api-1.7.7',
    ]
)

java_jar(
    name = 'java_sofa_pbrpc_test',
    srcs = [
        'src/com/cangyingzhijia/rpc/sofa/test',
    ],
    deps = [
        ':java_sofa_pbrpc',
        '//thirdparty/sofa/java/proto:proto'
    ]
)

java_jar(
    name = 'protobuf_java',
    srcs = [
        'src/com/google/protobuf',
    ],
)
