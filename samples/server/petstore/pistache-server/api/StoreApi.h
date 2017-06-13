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
/*
 * StoreApi.h
 *
 * 
 */

#ifndef StoreApi_H_
#define StoreApi_H_


#include <pistache/endpoint.h>
#include <pistache/http.h>
#include <pistache/router.h>

#include "Order.h"
#include <map>
#include <string>

namespace io {
namespace swagger {
namespace server {
namespace api {

using namespace io::swagger::server::model;

class  StoreApi {
public:
    StoreApi(Net::Address addr);
    virtual ~StoreApi() {};
    void init(size_t thr);
    void start();
    void shutdown();

    const std::string base = "/v2";

private:
    void setupRoutes();

    void delete_order_handler(const Net::Rest::Request &request, Net::Http::ResponseWriter response);
    void get_inventory_handler(const Net::Rest::Request &request, Net::Http::ResponseWriter response);
    void get_order_by_id_handler(const Net::Rest::Request &request, Net::Http::ResponseWriter response);
    void place_order_handler(const Net::Rest::Request &request, Net::Http::ResponseWriter response);
    void _default_handler(const Net::Rest::Request &request, Net::Http::ResponseWriter response);

    std::shared_ptr<Net::Http::Endpoint> httpEndpoint;
    Net::Rest::Router router;


    /// <summary>
    /// Delete purchase order by ID
    /// </summary>
    /// <remarks>
    /// For valid response try integer IDs with value &lt; 1000. Anything above 1000 or nonintegers will generate API errors
    /// </remarks>
    virtual void delete_order(const Net::Rest::Request &request, Net::Http::ResponseWriter &response) = 0;

    /// <summary>
    /// Returns pet inventories by status
    /// </summary>
    /// <remarks>
    /// Returns a map of status codes to quantities
    /// </remarks>
    virtual void get_inventory(const Net::Rest::Request &request, Net::Http::ResponseWriter &response) = 0;

    /// <summary>
    /// Find purchase order by ID
    /// </summary>
    /// <remarks>
    /// For valid response try integer IDs with value &lt;&#x3D; 5 or &gt; 10. Other values will generated exceptions
    /// </remarks>
    virtual void get_order_by_id(const Net::Rest::Request &request, Net::Http::ResponseWriter &response) = 0;

    /// <summary>
    /// Place an order for a pet
    /// </summary>
    /// <remarks>
    /// 
    /// </remarks>
    virtual void place_order(const Net::Rest::Request &request, Net::Http::ResponseWriter &response) = 0;

};

}
}
}
}

#endif /* StoreApi_H_ */

