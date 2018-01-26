//
// Name.swift
//
// Generated by swagger-codegen
// https://github.com/swagger-api/swagger-codegen
//

import Foundation


/** Model for testing model name same as property name */

public struct Name: Codable {

    public var name: Int
    public var snakeCase: Int?
    public var property: String?
    public var _123Number: Int?


    public enum CodingKeys: String, CodingKey { 
        case name
        case snakeCase = "snake_case"
        case property
        case _123Number = "123Number"
    }


}
