<template>
  <div class="register-page">
    <section class="form-contianer" v-if="!success">
      <div class="titleArea" @click="route.push('/')">
        <div id="register-title">
          <span class="title"> 注册<i></i></span>
        </div>
      </div>
      <a-form :model="formState" class="registerForm" ref="formRef" :rules="rules" layout="vertical">
        <a-form-item name="nickname" label="用户昵称" :label-col="{ span: 6 }">
          <a-input type="text" placeholder="请输入用户昵称" v-model:value="formState.nickname"></a-input>
        </a-form-item>
        <a-form-item name="username" label="用户名" :label-col="{ span: 6 }">
          <a-input type="text" placeholder="请输入用户名" v-model:value="formState.username"></a-input>
        </a-form-item>
        <a-form-item name="password" label="登陆密码" :label-col="{ span: 6 }">
          <a-input type="password" placeholder="请输入登陆密码" v-model:value="formState.password"></a-input>
        </a-form-item>
        <a-form-item name="passwordConfirm" label="确认密码" :label-col="{ span: 6 }">
          <a-input type="password" placeholder="请输入确认密码" v-model:value="formState.passwordConfirm"></a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="submitForm()" class="submit-btn">注册</a-button>
        </a-form-item>
        <div class="register">
          <router-link :to="{ path: '/login' }">已有账号，去登录</router-link>
        </div>
      </a-form>
    </section>
    <div class="result" v-else>
      <a-result status="success" :title="`注册成功，欢迎您加入Ant Live大家庭～`" sub-title="">
        <template #extra>
          <a-button key="console" type="primary" @click="route.push('/login')">去登录</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue"
import { message } from "ant-design-vue"
import { useRouter } from "vue-router"
import userApi from "@/api/user"

const route = useRouter()
const formRef = ref({})
const formState = reactive({
  nickname: "帅哥",
  username: "admin",
  password: "123123",
  passwordConfirm: "123123",
})
const success = ref(false)
const rules = {
  nickname: [{ required: true, message: "请输入用户昵称!" }],
  username: [{ required: true, message: "请输入用户名!" }],
  password: [{ required: true, message: "请输入登录密码!" }],
  passwordConfirm: [
    {
      required: true,
      message: "请输入确认密码!",
    },
    {
      validator: (rule, value, callback) => {
        if (value && value !== formState.password) {
          return Promise.reject("两次输入的密码不一致")
        }
        return Promise.resolve()
      },
    },
  ],
}
// 提交表单
const submitForm = () => {
  formRef.value
    .validateFields()
    .then(() => {
      userApi.register(formState).then((res) => {
        if (res.code === 0) {
          message.success("注册成功")
          success.value = true
        }
      })
    })
    .catch((error) => {
      // ignore
    })
}
</script>

<style lang="scss" scoped>
.register-page {
  position: absolute;
  width: 100%;
  height: 100%;
  background: url(@/assets/img/login.svg) no-repeat center center;
  background-size: 100% 100%;
  .result {
    display: flex;
    width: 100%;
    height: 100%;
    justify-content: center;
    align-items: center;
  }
  .form-contianer {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 370px;
    border-radius: 5px;
    padding: 25px;
    .titleArea {
      text-align: center;
      justify-content: center;
      align-items: center;
      // text-transform: uppercase;
      font-size: 22px;
      width: 100%;
      padding-bottom: 20px;
      .logo {
        width: 40px;
        margin-right: 10px;
      }
      .title {
        i {
          color: $primary-color;
          font-style: normal;
        }
      }
    }

    .registerForm {
      .submit-btn {
        width: 100%;
      }
    }
  }
  .register {
    text-decoration: none;
    text-align: right;
    a {
      text-decoration: none;
      padding: 5px;
      color: #666666;
    }
    a:visited {
      color: #666666;
    }
  }
  .register:hover {
    a {
      color: $primary-color;
    }
  }
  .manage_tip {
    margin-bottom: 20px;
    .title {
      font-family: cursive;
      font-weight: bold;
      font-size: 26px;
      color: #fff;
    }
    .logo {
      width: 60px;
      height: 60px;
    }
  }
  #register-title {
    cursor: pointer;
  }
}
</style>
