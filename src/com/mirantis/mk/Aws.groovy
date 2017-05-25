package com.mirantis.mk



/**
 *
 * AWS function functions
 *
 */

def setupVirtualEnv(venv_path = 'aws_venv') {
    def python = new com.mirantis.mk.Python()

    requirements = [
        'awscli'
    ]

    python.setupVirtualenv(venv_path, 'python2', requirements)
}

// ParameterKey=KeyName,ParameterValue=tkukral_yubi

//    env_vars = [
//        "AWS_ACCESS_KEY_ID=${access_key_id}",
//        "AWS_SECRET_ACCESS_KEY=${secret_access_key}",
//        "AWS_DEFAULT_REGION=${region}"
//    ]


def createStack(vevn_path, env_vars, template_file, parameters = []) {
    def python = new com.mirantis.mk.Python()


    def cmd = "aws cloudformation create-stack --stack-name ${stack_name} --template-body file://${template_file}"

    if (parameters != null && parameters.length > 0) {
        cmd = "${cmd} --parameters"

        for (int i=0; i<parameters.length; i++) {
           cmd = "${cmd} ${parameters[i]}"
        }
    }

    withEnv(envVars) {
        print(cmd)
        def out = python.runVirtualenvCommand(venv_path, cmd)
    }
}

def describeStack(venv_path, env_vars, stack_name) {
    def python = new com.mirantis.mk.Python()

    def envVars = [
        "AWS_ACCESS_KEY_ID=${access_key_id}",
        "AWS_SECRET_ACCESS_KEY=${secret_access_key}",
        "AWS_DEFAULT_REGION=${region}"
    ]

    cmd = "aws cloudformation describe-stacks --stack-name ${stack_name}"

    withEnv(envVars) {
        print(cmd)
        def out = python.runVirtualenvCommand(venv_path, cmd)
        def stack_info = out['Stacks'][0]
        print(stack_info)
        return stack_info
    }

    return {}
}

def waitForStatus(venv_path, env_vars, stack_name, state, timeout = 600) {
    def python = new com.mirantis.mk.Python()
    def common = new com.mirantis.mk.Common()

    timeout = timeout * 1000
    Date date = new Date()
    def time_start = date.getTime() // in seconds

    while (true) {
        // get stack state
        withEnv(envVars) {
            stack_info = describeStack(venv_path, env_vars, stack_name)
            print(stack_info)
            if (stack_info['StackStatus'] == state) {
                common.successMsg("Stack ${stack_name} in in state ${state}")
                break;
            }
        }

        // check for timeout
        if (time_start + timeout < date.getTime()) {
            throw new Exception("Timeout while waiting for state ${state} for stack ${stack}")
        }
    }
}
