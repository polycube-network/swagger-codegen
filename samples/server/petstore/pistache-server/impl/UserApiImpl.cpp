/**
* Swagger Petstore
* This is a sample server Petstore server.  You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you can use the api key `special-key` to test the authorization filters.
*
* OpenAPI spec version: 1.0.0
* Contact: apiteam@swagger.io
*
* NOTE: This class is auto generated by the swagger code generator program.
* https://github.com/swagger-api/swagger-codegen.git
* Do not edit the class manually.
*/

#include "UserApiImpl.h"

namespace io {
namespace swagger {
namespace server {
namespace api {

using namespace io::swagger::server::model;

UserApiImpl::UserApiImpl(Net::Address addr)
    : UserApi(addr)
    { }

void UserApiImpl::create_user(const User &body, Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}
void UserApiImpl::create_users_with_array_input(const User &body, Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}
void UserApiImpl::create_users_with_list_input(const User &body, Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}
void UserApiImpl::delete_user(const std::string &username, Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}
void UserApiImpl::get_user_by_name(const std::string &username, Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}
void UserApiImpl::login_user(const Optional<null> &username, const Optional<null> &password, Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}
void UserApiImpl::logout_user(Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}
void UserApiImpl::update_user(const std::string &username, const User &body, Net::Http::ResponseWriter &response) {
    response.send(Net::Http::Code::Ok, "Do some magic");
}

}
}
}
}

