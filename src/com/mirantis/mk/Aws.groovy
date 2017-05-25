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

def getEnvVars(credentials_id, region = 'us-west-2') {
    def common = new com.mirantis.mk.Common()

    def creds = common.getCredentials(credentials_id)

    return [
        "AWS_ACCESS_KEY_ID=${creds.username}",
        "AWS_SECRET_ACCESS_KEY=${creds.password}",
        "AWS_DEFAULT_REGION=${region}"
    ]

}

// ParameterKey=KeyName,ParameterValue=tkukral_yubi



def createStack(venv_path, env_vars, template_file, stack_name, parameters = []) {
    def python = new com.mirantis.mk.Python()


    def cmd = "aws cloudformation create-stack --stack-name ${stack_name} --template-body file://template/${template_file}"

    if (parameters != null && parameters.size() > 0) {
        cmd = "${cmd} --parameters"

        for (int i=0; i<parameters.size(); i++) {
           cmd = "${cmd} ${parameters[i]}"
        }
    }

    withEnv(env_vars) {
        print(cmd)
        def out = python.runVirtualenvCommand(venv_path, cmd)
    }
}

def describeStack(venv_path, env_vars, stack_name) {
    def python = new com.mirantis.mk.Python()
    def common = new com.mirantis.mk.Common()

    cmd = "aws cloudformation describe-stacks --stack-name ${stack_name}"

    withEnv(env_vars) {
        print(cmd)
        def out = python.runVirtualenvCommand(venv_path, cmd)
        def out_json = common.parseJSON(out)
        def stack_info = out_json['Stacks'][0]
        print(stack_info)
        return stack_info
    }
}

def waitForStatus(venv_path, env_vars, stack_name, state, timeout = 600, loop_sleep = 30) {
    def aws = new com.mirantis.mk.Aws()
    def common = new com.mirantis.mk.Common()
    def python = new com.mirantis.mk.Python()

    timeout = timeout * 1000
    Date date = new Date()
    def time_start = date.getTime() // in seconds

    while (true) {
        // get stack state
        withEnv(env_vars) {
            stack_info = aws.describeStack(venv_path, env_vars, stack_name)
            common.infoMsg('Stack status is ' + stack_info['StackStatus'])

            if (stack_info['StackStatus'] == state) {
                common.successMsg("Stack ${stack_name} in in state ${state}")
                common.prettyPrint(stack_info)
                break
            }
        }

        // check for timeout
        if (time_start + timeout < date.getTime()) {
            throw new Exception("Timeout while waiting for state ${state} for stack ${stack}")
        } else {
            common.infoMsg('Waiting for stack to start. Elapsed ' + (date.getTime() - time_start) + ' seconds')
        }

        // wait for next loop
        sleep(loop_sleep)
    }
}
