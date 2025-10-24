import { Select, SelectItem } from "@carbon/react";

const CustomSelect = (props) => {
  const handleSelect = (e) => {
    const value = e.target.value;
    props.onChange(value);
  };
  return (
    <>
      <Select
        onChange={handleSelect}
        labelText=""
        id={props.id}
        defaultValue={props.value ? props.value : null}
        disabled={props.disabled}
      >
        {props.defaultSelect && (
          <SelectItem
            text={props.defaultSelect.value}
            value={props.defaultSelect.id}
          />
        )}

        {props.options != null &&
          props.options.map((option, index) => {
            return (
              <SelectItem key={index} text={option.value} value={option.id} />
            );
          })}
      </Select>
    </>
  );
};

export default CustomSelect;
