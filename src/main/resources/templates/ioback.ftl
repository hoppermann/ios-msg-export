<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <style>

      body {
        background: white;
        font-family: sans-serif;
        font-size: 14px;
      }

      img {
        image-orientation: from-image;
      }

      blockquote {
        margin: 0 auto;
        max-width: 400px;
      }

      p {
        margin: 0 0 0.5em;
        border-radius: 1em;
        padding: 0.5em 1em;
        max-width: 75%;
        clear: both;
        position: relative;
      }

      p.them {
        float: left;
      }

      p.them::after {
        content: \"\";
        position: absolute;
        left: -0.5em;
        bottom: 0;
        width: 0.5em;
        height: 1em;
        border-right-width: 0.5em;
        border-right-style: solid;
        border-bottom-right-radius: 1em 0.5em;
      }

      p.me {
        float: right;
      }

      p.me::after {
        content: \"\";
        position: absolute;
        right: -0.5em;
        bottom: 0;
        width: 0.5em;
        height: 1em;
        border-left-width: 0.5em;
        border-left-style: solid;
        border-bottom-left-radius: 1em 0.5em;
      }

      .me-sms {
        background-color: #1bbd1c;
        color: #f0f0f0;
        border-right-color: #1ab31b;
      }

      .me-iMessage {
        background-color: #5783bd;
        color: #f0f0f0;
        border-left-color: #517bb1;
      }

      .them-sms {
        background-color: #dce9d8;
        color: #000000;
        border-left-color: #cfdbcb;
      }

      .them-iMessage {
        background-color: #cdd1d9;
        color: #000000;
        border-left-color: #c4c7cf;
      }

      p span {
        font-size: 70%;
      }
    </style>

    <title>${contact.name}</title>
  </head>
  <body>
  <#setting datetime_format="dd.MM.yyyy HH:mm:ss">
    <h1>${contact.name}</h1>

    <blockquote class="chat">
    <#list messages as message>
      <p class="${(message.type==0)?then("them", "me")} ${(message.type==0)?then("them", "me")}-${(message.sms)?then("sms", "iMessage")}">
        <#if message.imageData??><img style="max-width: 300px" src="${message.imageData}"/></#if>
      ${message.text}
        <br/>
        <span>${message.date?datetime}</span></p>
    </#list>
    </blockquote>
  </body>

</html>

