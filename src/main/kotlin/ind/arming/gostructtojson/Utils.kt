package ind.arming.gostructtojson

import com.goide.psi.*
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern



object Utils {
    private val basicTypes: MutableMap<String, Any> = HashMap()
    private const val STRUCT_TYPE = "STRUCT_TYPE"

    init {
        basicTypes["bool"] = false
        basicTypes["byte"] = 0
        basicTypes["int"] = 0
        basicTypes["uint"] = 0
        basicTypes["uint8"] = 255
        basicTypes["uint16"] = 65535
        basicTypes["uint32"] = 4294967295L
        basicTypes["uint64"] = 1844674407370955161L
        basicTypes["int8"] = -128
        basicTypes["int16"] = -32768
        basicTypes["int32"] = -2147483648
        basicTypes["int64"] = -9223372036854775807L
        basicTypes["uintptr"] = 0 //uintptr is an integer type that is large enough to hold the bit pattern of any pointer
        basicTypes["rune"] = 0 // rune is an alias for int32 and is equivalent to int32 in all ways
        basicTypes["long"] = 0L
        basicTypes["float32"] = -0.2f
        basicTypes["float64"] = -0.1f
        basicTypes["string"] = "demoString"
        basicTypes["time.Time"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    }

    fun isBasicType(typeName: String): Boolean {
        return basicTypes.containsKey(typeName)
    }

    fun convertGoStructToJson(goStructType: GoStructType?): String {
        val map = getKVMap(goStructType)
        return GsonBuilder().setPrettyPrinting().create().toJson(map)
    }

    /*
    // The encoding of each struct field can be customized by the format string
    // stored under the "json" key in the struct field's tag.
    // As a special case, if the field tag is "-", the field is always omitted.
    //
    //  Field int `json:"myName"` -->Field appears in JSON as key "myName".
    //  Field int `json:"myName,omitempty"` -->Field appears in JSON as key "myName" and the field is omitted from the object if its value is empty,
    //   Field int `json:"-"`    -->  Field is ignored by this package
    //   Field int `json:"-"`    --> Field is ignored by this package.
    //   Field int `json:"-,"`   --> Field appears in JSON as key "-".
     */
    private fun getJsonKeyName(fieldName: String, tagText: String?): String {
        var jsonKey = fieldName
        if (tagText == null || tagText == "") {
            return jsonKey
        }
        val regPattern = "[json|redis]:\"([\\w\\d_,-\\.]+)\""
        val pattern = Pattern.compile(regPattern)
        val matcher = pattern.matcher(tagText)
        if (matcher.find()) {
            val tmpKeyName = matcher.group(1).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            if (tmpKeyName != "-") { // for now,don't omit any field
                jsonKey = tmpKeyName
            }
        }
        return jsonKey
    }

    /**
     * demo struct:
     * type Person struct {
     * Name string `json:"name"`
     * Age  int    `json:"age"`
     * Addr string `json:"addr"`
     * }
     *
     *
     * type Employee struct {
     * Person
     * salary int `json:"salary"`
     * Dep    struct {
     * Number int    `json:"dep_number"`
     * Name   string `json:"dep_name"`
     * } `json:"dep"`
     * }
     *
     * @param goStructType
     * @return
     */
    private fun getKVMap(goStructType: GoStructType?): Map<String, Any?> {
        val map: MutableMap<String, Any?> = LinkedHashMap()

        val fieldsDeclareList = goStructType!!.fieldDeclarationList

        for (field in fieldsDeclareList) {
            val fieldType = field.type
            if (fieldType == null) {  // to deal with Person in Employee
                val anonymous = field.anonymousFieldDefinition
                if (anonymous != null) {
                    val typeRef = anonymous.typeReferenceExpression
                    val resolve = typeRef?.resolve()
                    if (resolve is GoTypeSpec) {
                        val type = resolve.specType.type
                        if (type is GoStructType) {
                            val tmpMap = getKVMap(type as GoStructType?)
                            map.putAll(tmpMap)
                        }
                    }
                }
            } else {
                val fieldName = field.fieldDefinitionList[0].identifier.text
                val fieldTagText = field.tagText

                val jsonKey = getJsonKeyName(fieldName, fieldTagText)

                map[jsonKey] = getCustomType(fieldType)
            }
        }
        return map
    }

    private fun getCustomType(fieldType: GoType): Any? {
        if (fieldType == null) {  // to deal with Person in Employee
            return ""
        } else {
            val typeRef = fieldType.typeReferenceExpression
            val fieldTypeStr = if (typeRef == null) "NOTBASICTYPE" else typeRef.text

            if (isBasicType(fieldTypeStr)) {
                return basicTypes[fieldTypeStr]
            } else if (fieldType is GoStructType) {
                val tmpMap = getKVMap(fieldType as GoStructType?)
                return tmpMap
            } else if (fieldType is GoMapType) {
                val tmpMap: MutableMap<String, Any?> = HashMap()
                // key type default to be string
                val tmpValueType = Objects.requireNonNull(fieldType.valueType)!!.text
                if (isBasicType(tmpValueType)) {
                    tmpMap["demoKey"] = basicTypes[tmpValueType]
                } else {
                    val  tt = fieldType.valueType
                    if (tt!=null){
                        tmpMap["demoKey"] = getCustomType(tt)
                    }
                }
                return tmpMap
            } else if (fieldType is GoArrayOrSliceType) {
                val tmpList = ArrayList<Any?>()
                val tmpStr = fieldType.type.text
                if (isBasicType(tmpStr)) {
                    tmpList.add(basicTypes[tmpStr])
                } else {
                    val  tt = fieldType.type
                    tt?.let { tmpList.add(getCustomType(tt))}
                }
                return tmpList
            } else if (fieldType is GoPointerType) {
                val  tt = fieldType.type
                return tt?.let { getCustomType(it) }
            } else if (fieldType is GoInterfaceType) {
                return HashMap<Any, Any>()
            } else {
                val underType = fieldType.contextlessUnderlyingType
                return getCustomType(underType)
            }
        }
    }
}